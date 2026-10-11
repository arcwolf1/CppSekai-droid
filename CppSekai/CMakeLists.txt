cmake_minimum_required(VERSION 3.16)
project(cppsekai CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(SDL2_PREBUILT ${CMAKE_SOURCE_DIR}/prebuilt)
add_library(SDL2 SHARED IMPORTED)
set_target_properties(SDL2 PROPERTIES
    IMPORTED_LOCATION ${SDL2_PREBUILT}/${CMAKE_ANDROID_ARCH_ABI}/libSDL2.so
    INTERFACE_INCLUDE_DIRECTORIES ${SDL2_PREBUILT}/include)

find_library(EGL_LIB      EGL)
find_library(GLES_LIB     GLESv2)
find_library(LOG_LIB      log)
find_library(ANDROID_LIB  android)
find_library(OPENSLES_LIB OpenSLES)

# ---- 上游 AGPL 代码（-w）
set(UPSTREAM_SOURCES
    core/native/src/mmw_preview.cpp
    core/native/mmw_port/Math.cpp
    core/native/mmw_port/MinMax.cpp
    core/native/mmw_port/Note.cpp
    core/native/mmw_port/Score.cpp
    core/native/mmw_port/Tempo.cpp
    core/native/mmw_port/Utilities.cpp
    core/native/mmw_port/EffectView.cpp
    core/native/mmw_port/Particle.cpp
    core/native/mmw_port/ResourceManager.cpp
    core/native/mmw_port/Rendering/Camera.cpp
)
add_library(cppsekai_upstream STATIC ${UPSTREAM_SOURCES})
target_include_directories(cppsekai_upstream PUBLIC
    core/native core/native/src core/native/generated
    third_party third_party/nlohmann core/native/vendor
    third_party/DirectXMath/Inc)
target_compile_options(cppsekai_upstream PRIVATE -w -D_XM_NO_INTRINSICS_)
set_target_properties(cppsekai_upstream PROPERTIES POSITION_INDEPENDENT_CODE ON)
# ---- imgui（扁平布局）
add_library(imgui STATIC
    third_party/imgui/imgui.cpp
    third_party/imgui/imgui_draw.cpp
    third_party/imgui/imgui_tables.cpp
    third_party/imgui/imgui_widgets.cpp
    third_party/imgui/imgui_impl_sdl2.cpp
    third_party/imgui/imgui_impl_opengl3.cpp)
target_include_directories(imgui PUBLIC third_party/imgui)
target_link_libraries(imgui PUBLIC SDL2)
target_compile_options(imgui PRIVATE -w)
target_compile_definitions(imgui PUBLIC IMGUI_IMPL_OPENGL_ES2)
target_compile_definitions(imgui PUBLIC ImTextureID=void*)
set_target_properties(cppsekai_upstream PROPERTIES POSITION_INDEPENDENT_CODE ON)
# ---- 游戏本体
set(GAME_SOURCES
    main.cpp
    platform/Audio.cpp
    platform/CoreApi.cpp
    platform/FontOutline.cpp
    platform/Party_android.cpp
    platform/Renderer.cpp
    # platform/gl_stub_android.cpp
    platform/SystemMedia_android.cpp
    game/Hud.cpp game/Intro.cpp game/Judgement.cpp game/PartyScreen.cpp
    game/Result.cpp game/SongSelect.cpp game/StageBackground.cpp
    game/TapEffect.cpp game/Ui.cpp)

add_library(main SHARED ${GAME_SOURCES})
target_include_directories(main PRIVATE
    . core/native core/native/src core/native/generated
    third_party third_party/nlohmann third_party/DirectXMath/Inc)
target_compile_options(main PRIVATE
    -Wall -Wextra -Wno-unused-parameter -Wno-missing-field-initializers
    -D_XM_NO_INTRINSICS_)
target_link_libraries(main
    cppsekai_upstream imgui SDL2
    ${EGL_LIB} ${GLES_LIB} ${LOG_LIB} ${ANDROID_LIB} ${OPENSLES_LIB})
