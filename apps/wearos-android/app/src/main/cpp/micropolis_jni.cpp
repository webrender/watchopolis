// JNI bridge between the Kotlin app and the portable C++ Micropolis engine.
//
// Phase 1: minimal smoke test — create an engine, generate a random city, and
// read a tile back. Later phases add bulk map access, simulation ticks, tools,
// and callbacks forwarded to Kotlin.
//
// The engine's own ConsoleCallback (callback.cpp) is Emscripten-only — its
// methods call JS via EM_ASM_ — so this file provides a native Callback instead.

#include <jni.h>
#include <android/log.h>

#include <cstdlib>
#include <new>

#include "micropolis.h"

#define LOG_TAG "MicropolisJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

namespace {

// Cached so callbacks (which fire on the Kotlin tick thread) can reach the JVM.
JavaVM *g_vm = nullptr;

// Owns a simulation instance plus the up-call references to the Kotlin
// MicropolisEngine. Micropolis::setCallback() takes ownership of the callback
// and deletes it in ~Micropolis(), so the handle must not delete it.
struct EngineHandle {
    Micropolis *sim = nullptr;
    jobject engineRef = nullptr;       // global ref to the Kotlin MicropolisEngine
    jmethodID onSoundMethod = nullptr;
    jmethodID onMessageMethod = nullptr;
    // Scratch for the synchronous query tool: showZoneStatus() (fired from inside
    // doTool) writes [category, density, landValue, crime, pollution, growth] here
    // and sets the valid flag; nativeQueryZone reads it back immediately.
    int zoneStatus[6] = {0};
    bool zoneStatusValid = false;
};

// Forwards selected engine notifications to the Kotlin engine object; the rest
// are no-ops so the engine never hits a null callback.
class NativeCallback : public Callback {
public:
    EngineHandle *parent = nullptr;

    void makeSound(Micropolis *, emscripten::val, std::string channel,
                   std::string sound, int x, int y) override {
        if (parent == nullptr || parent->onSoundMethod == nullptr) return;
        JNIEnv *env = nullptr;
        if (g_vm == nullptr ||
            g_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        jstring jc = env->NewStringUTF(channel.c_str());
        jstring js = env->NewStringUTF(sound.c_str());
        env->CallVoidMethod(parent->engineRef, parent->onSoundMethod, jc, js, (jint)x, (jint)y);
        env->DeleteLocalRef(jc);
        env->DeleteLocalRef(js);
    }
    void sendMessage(Micropolis *, emscripten::val, int messageIndex, int x, int y,
                     bool, bool) override {
        if (parent == nullptr || parent->onMessageMethod == nullptr) return;
        JNIEnv *env = nullptr;
        if (g_vm == nullptr ||
            g_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        env->CallVoidMethod(parent->engineRef, parent->onMessageMethod,
                            (jint)messageIndex, (jint)x, (jint)y);
    }

    void autoGoto(Micropolis *, emscripten::val, int, int, std::string) override {}
    void didGenerateMap(Micropolis *, emscripten::val, int) override {}
    void didLoadCity(Micropolis *, emscripten::val, std::string) override {}
    void didLoadScenario(Micropolis *, emscripten::val, std::string, std::string) override {}
    void didLoseGame(Micropolis *, emscripten::val) override {}
    void didSaveCity(Micropolis *, emscripten::val, std::string) override {}
    void didTool(Micropolis *, emscripten::val, std::string, int, int) override {}
    void didWinGame(Micropolis *, emscripten::val) override {}
    void didntLoadCity(Micropolis *, emscripten::val, std::string) override {}
    void didntSaveCity(Micropolis *, emscripten::val, std::string) override {}
    void newGame(Micropolis *, emscripten::val) override {}
    void saveCityAs(Micropolis *, emscripten::val, std::string) override {}
    void showBudgetAndWait(Micropolis *, emscripten::val) override {}
    void showZoneStatus(Micropolis *, emscripten::val, int tileCategory,
                        int populationDensity, int landValue, int crime,
                        int pollution, int growth, int, int) override {
        if (parent == nullptr) return;
        parent->zoneStatus[0] = tileCategory;
        parent->zoneStatus[1] = populationDensity;
        parent->zoneStatus[2] = landValue;
        parent->zoneStatus[3] = crime;
        parent->zoneStatus[4] = pollution;
        parent->zoneStatus[5] = growth;
        parent->zoneStatusValid = true;
    }
    void simulateRobots(Micropolis *, emscripten::val) override {}
    void simulateChurch(Micropolis *, emscripten::val, int, int, int) override {}
    void startEarthquake(Micropolis *, emscripten::val, int) override {}
    void startGame(Micropolis *, emscripten::val) override {}
    void startScenario(Micropolis *, emscripten::val, int) override {}
    void updateBudget(Micropolis *, emscripten::val) override {}
    void updateCityName(Micropolis *, emscripten::val, std::string) override {}
    void updateDate(Micropolis *, emscripten::val, int, int) override {}
    void updateDemand(Micropolis *, emscripten::val, float, float, float) override {}
    void updateEvaluation(Micropolis *, emscripten::val) override {}
    void updateFunds(Micropolis *, emscripten::val, int) override {}
    void updateGameLevel(Micropolis *, emscripten::val, int) override {}
    void updateHistory(Micropolis *, emscripten::val) override {}
    void updateMap(Micropolis *, emscripten::val) override {}
    void updateOptions(Micropolis *, emscripten::val) override {}
    void updatePasses(Micropolis *, emscripten::val, int) override {}
    void updatePaused(Micropolis *, emscripten::val, bool) override {}
    void updateSpeed(Micropolis *, emscripten::val, int) override {}
    void updateTaxRate(Micropolis *, emscripten::val, int) override {}
};

inline EngineHandle *fromHandle(jlong handle) {
    return reinterpret_cast<EngineHandle *>(handle);
}

} // namespace

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeCreate(JNIEnv *env, jobject thiz) {
    auto *h = new EngineHandle();
    // The engine assumes zero-initialized memory (it always gets that under
    // Emscripten/WASM). Its constructor leaves `callback` and many members
    // uninitialized, so allocate zeroed storage and placement-new into it.
    void *mem = std::calloc(1, sizeof(Micropolis));
    h->sim = new (mem) Micropolis();
    // Set the callback before init() so engine notifications never hit a null.
    // The engine takes ownership and frees it in its destructor.
    auto *cb = new NativeCallback();
    cb->parent = h;
    h->sim->setCallback(cb, emscripten::val());
    h->sim->init();
    // Up-call wiring: keep a global ref to the Kotlin engine and resolve the
    // methods the native callback forwards to.
    h->engineRef = env->NewGlobalRef(thiz);
    jclass cls = env->GetObjectClass(thiz);
    h->onSoundMethod =
        env->GetMethodID(cls, "onNativeSound", "(Ljava/lang/String;Ljava/lang/String;II)V");
    h->onMessageMethod = env->GetMethodID(cls, "onNativeMessage", "(III)V");
    LOGI("Micropolis engine created");
    return reinterpret_cast<jlong>(h);
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGenerateRandomCity(
        JNIEnv *, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->generateSomeRandomCity();
    LOGI("Generated random city");
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetGameLevelFunds(
        JNIEnv *, jobject, jlong handle, jint level) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->setGameLevelFunds(static_cast<GameLevel>(level));
    LOGI("setGameLevelFunds(%d)", level);
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSaveCity(
        JNIEnv *env, jobject, jlong handle, jstring path) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || path == nullptr) return;
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    h->sim->saveCityAs(std::string(cpath));
    LOGI("saveCityAs(%s)", cpath);
    env->ReleaseStringUTFChars(path, cpath);
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetCityName(
        JNIEnv *env, jobject, jlong handle, jstring name) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || name == nullptr) return;
    const char *cname = env->GetStringUTFChars(name, nullptr);
    h->sim->setCityName(std::string(cname));
    LOGI("setCityName(%s)", cname);
    env->ReleaseStringUTFChars(name, cname);
}

JNIEXPORT jint JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetTile(
        JNIEnv *, jobject, jlong handle, jint x, jint y) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return -1;
    return h->sim->getTile(x, y);
}

JNIEXPORT jboolean JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeLoadCity(
        JNIEnv *env, jobject, jlong handle, jstring path) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || path == nullptr) return JNI_FALSE;
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    bool ok = h->sim->loadCity(std::string(cpath));
    env->ReleaseStringUTFChars(path, cpath);
    LOGI("loadCity(%s) -> %d", cpath, ok);
    return ok ? JNI_TRUE : JNI_FALSE;
}

// Apply an editing tool at a tile. Returns the ToolResult (1 = ok, 0 = failed,
// -1 = need bulldoze, -2 = no money).
JNIEXPORT jint JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeDoTool(
        JNIEnv *, jobject, jlong handle, jint tool, jint x, jint y) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return 0;
    return static_cast<jint>(
        h->sim->doTool(static_cast<EditingTool>(tool),
                       static_cast<short>(x), static_cast<short>(y)));
}

// Run the query tool at a tile and read back the zone status it produces. The
// engine reports the status via the (synchronous) showZoneStatus callback, which
// stashes it on the handle; we copy [category, density, landValue, crime,
// pollution, growth] into out6. Returns false if the tile was out of bounds (the
// engine bails before reporting), true otherwise.
JNIEXPORT jboolean JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeQueryZone(
        JNIEnv *env, jobject, jlong handle, jint x, jint y, jintArray out6) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || out6 == nullptr) return JNI_FALSE;
    h->zoneStatusValid = false;
    h->sim->doTool(static_cast<EditingTool>(5 /* TOOL_QUERY */),
                   static_cast<short>(x), static_cast<short>(y));
    if (!h->zoneStatusValid) return JNI_FALSE;
    jint v[6];
    for (int i = 0; i < 6; i++) v[i] = h->zoneStatus[i];
    env->SetIntArrayRegion(out6, 0, 6, v);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSimTick(
        JNIEnv *, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->simTick();
    h->sim->animateTiles();
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetPasses(
        JNIEnv *, jobject, jlong handle, jint passes) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->setPasses(passes);
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetSpeed(
        JNIEnv *, jobject, jlong handle, jint speed) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->setSpeed(static_cast<short>(speed));
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetSound(
        JNIEnv *, jobject, jlong handle, jboolean enabled) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->setEnableSound(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetPaused(
        JNIEnv *, jobject, jlong handle, jboolean paused) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->simPaused = (paused == JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetYear(
        JNIEnv *, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    return (h && h->sim) ? static_cast<jint>(h->sim->cityYear) : 0;
}

JNIEXPORT jint JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetMonth(
        JNIEnv *, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    return (h && h->sim) ? static_cast<jint>(h->sim->cityMonth) : 0;
}

JNIEXPORT jlong JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetFunds(
        JNIEnv *, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    return (h && h->sim) ? static_cast<jlong>(h->sim->totalFunds) : 0;
}

JNIEXPORT jlong JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetPopulation(
        JNIEnv *, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    return (h && h->sim) ? static_cast<jlong>(h->sim->cityPop) : 0;
}

JNIEXPORT jstring JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetCityName(
        JNIEnv *env, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    const char *name = (h && h->sim) ? h->sim->cityName.c_str() : "";
    return env->NewStringUTF(name);
}

// Bulk-copy the whole tile map into a Kotlin ShortArray. The engine stores the
// map column-major in a contiguous buffer (index = x * WORLD_H + y).
JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeCopyMap(
        JNIEnv *env, jobject, jlong handle, jshortArray dest) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || dest == nullptr) return;
    const auto *src = reinterpret_cast<const jshort *>(h->sim->getMapAddress());
    if (src == nullptr) return;
    jsize want = static_cast<jsize>(h->sim->getMapSize() / sizeof(short));
    jsize have = env->GetArrayLength(dest);
    if (want > have) want = have;
    env->SetShortArrayRegion(dest, 0, want, src);
}

// Fill out6 = [cityTax, totalFunds, taxFund, roadFund, fireFund, policeFund]
// and out3 = [roadPercent, firePercent, policePercent].
JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetBudget(
        JNIEnv *env, jobject, jlong handle, jlongArray out6, jfloatArray out3) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    Micropolis *m = h->sim;
    jlong longs[6] = {
        (jlong)m->cityTax, (jlong)m->totalFunds, (jlong)m->taxFund,
        (jlong)m->roadFund, (jlong)m->fireFund, (jlong)m->policeFund,
    };
    jfloat floats[3] = { m->roadPercent, m->firePercent, m->policePercent };
    if (out6 != nullptr) env->SetLongArrayRegion(out6, 0, 6, longs);
    if (out3 != nullptr) env->SetFloatArrayRegion(out3, 0, 3, floats);
}

// Fill out12 = [score, scoreDelta, cityClass, approval(cityYes),
//   problem0..3 (number, -1 if none), problemVotes0..3].
JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetEvaluation(
        JNIEnv *env, jobject, jlong handle, jintArray out12) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || out12 == nullptr) return;
    Micropolis *m = h->sim;
    jint v[12];
    v[0] = m->cityScore;
    v[1] = m->cityScoreDelta;
    v[2] = (jint)m->cityClass;
    v[3] = m->cityYes;
    for (int k = 0; k < 4; k++) {
        v[4 + k] = m->getProblemNumber(k);
        v[8 + k] = m->getProblemVotes(k);
    }
    env->SetIntArrayRegion(out12, 0, 12, v);
}

// Fill out3 = [resDemand, comDemand, indDemand], each normalized -1..1.
JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeGetDemands(
        JNIEnv *env, jobject, jlong handle, jfloatArray out3) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr || out3 == nullptr) return;
    float res, com, ind;
    h->sim->getDemands(&res, &com, &ind);
    jfloat v[3] = {res, com, ind};
    env->SetFloatArrayRegion(out3, 0, 3, v);
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetTax(
        JNIEnv *, jobject, jlong handle, jint tax) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->setCityTax(static_cast<short>(tax));
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeSetFunding(
        JNIEnv *, jobject, jlong handle, jfloat road, jfloat fire, jfloat police) {
    auto *h = fromHandle(handle);
    if (h == nullptr || h->sim == nullptr) return;
    h->sim->roadPercent = road;
    h->sim->firePercent = fire;
    h->sim->policePercent = police;
}

JNIEXPORT void JNICALL
Java_com_watchopolis_wear_engine_MicropolisEngine_nativeDestroy(
        JNIEnv *env, jobject, jlong handle) {
    auto *h = fromHandle(handle);
    if (h == nullptr) return;
    if (h->sim != nullptr) {
        h->sim->~Micropolis();  // also deletes the callback it owns
        std::free(h->sim);      // paired with calloc in nativeCreate
    }
    if (h->engineRef != nullptr) env->DeleteGlobalRef(h->engineRef);
    delete h;
}

} // extern "C"
