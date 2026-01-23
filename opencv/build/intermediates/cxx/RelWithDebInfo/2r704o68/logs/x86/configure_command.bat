@echo off
"D:\\Study\\Ky_thuat_lap_trinh\\Android_Development_tools\\AndroidSDK\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\Study\\Ky_thuat_lap_trinh\\Smol_Project\\Face Checker 3.0\\opencv\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86" ^
  "-DANDROID_NDK=D:\\Study\\Ky_thuat_lap_trinh\\Android_Development_tools\\AndroidSDK\\ndk\\25.1.8937393" ^
  "-DCMAKE_ANDROID_NDK=D:\\Study\\Ky_thuat_lap_trinh\\Android_Development_tools\\AndroidSDK\\ndk\\25.1.8937393" ^
  "-DCMAKE_TOOLCHAIN_FILE=D:\\Study\\Ky_thuat_lap_trinh\\Android_Development_tools\\AndroidSDK\\ndk\\25.1.8937393\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=D:\\Study\\Ky_thuat_lap_trinh\\Android_Development_tools\\AndroidSDK\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\Study\\Ky_thuat_lap_trinh\\Smol_Project\\Face Checker 3.0\\opencv\\build\\intermediates\\cxx\\RelWithDebInfo\\2r704o68\\obj\\x86" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\Study\\Ky_thuat_lap_trinh\\Smol_Project\\Face Checker 3.0\\opencv\\build\\intermediates\\cxx\\RelWithDebInfo\\2r704o68\\obj\\x86" ^
  "-DCMAKE_BUILD_TYPE=RelWithDebInfo" ^
  "-BD:\\Study\\Ky_thuat_lap_trinh\\Smol_Project\\Face Checker 3.0\\opencv\\.cxx\\RelWithDebInfo\\2r704o68\\x86" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
