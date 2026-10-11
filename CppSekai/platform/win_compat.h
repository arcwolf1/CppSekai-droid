#pragma once

#ifdef __ANDROID__

#include <cstdint>
#include <cwchar>
#include <string>

using HANDLE   = void*;
using HWND     = void*;
using HINSTANCE= void*;
using HMODULE  = void*;
using DWORD    = uint32_t;
using WORD     = uint16_t;
using BYTE     = uint8_t;
using BOOL     = int;
using UINT     = unsigned int;
using ATOM     = uint16_t;
using LRESULT  = long;
using WPARAM   = unsigned long;
using LPARAM   = long;
using LPVOID   = void*;
using LPCVOID  = const void*;
using LPWSTR   = wchar_t*;
using LPCWSTR  = const wchar_t*;
using LPSTR    = char*;
using LPCSTR   = const char*;

#ifndef TRUE
#define TRUE 1
#endif
#ifndef FALSE
#define FALSE 0
#endif
#define CALLBACK
#define WINAPI
#define APIENTRY
#define ERROR_ALREADY_EXISTS 183
#define SW_SHOW    5
#define SW_RESTORE 9
#define SW_HIDE    0

inline HANDLE CreateMutexW(void*, BOOL, const wchar_t*) { return nullptr; }
inline BOOL   CloseHandle(HANDLE) { return TRUE; }
inline DWORD  GetLastError() { return 0; }
inline HWND   FindWindowW(const wchar_t*, const wchar_t*) { return nullptr; }
inline HWND   GetForegroundWindow() { return nullptr; }
inline BOOL   ShowWindow(HWND, int) { return TRUE; }
inline BOOL   SetForegroundWindow(HWND) { return TRUE; }
inline DWORD  GetWindowThreadProcessId(HWND, DWORD*) { return 0; }
inline BOOL   AttachThreadInput(DWORD, DWORD, BOOL) { return TRUE; }
inline BOOL   SetWindowPos(HWND, HWND, int, int, int, int, UINT) { return TRUE; }
inline BOOL   IsWindowVisible(HWND) { return FALSE; }
inline BOOL   IsIconic(HWND) { return FALSE; }
inline HWND   GetActiveWindow() { return nullptr; }

// ---- 窗口枚举（EnumWindows 一族）
#define GW_OWNER 4
inline HWND GetWindow(HWND, UINT) { return nullptr; }
inline int  GetWindowTextW(HWND, wchar_t*, int) { return 0; }
inline BOOL EnumWindows(BOOL(*)(HWND, LPARAM), LPARAM) { return TRUE; }

// ---- 路径
#ifndef MAX_PATH
#define MAX_PATH 260
#endif
inline DWORD GetModuleFileNameW(HMODULE, wchar_t*, DWORD) { return 0; }

// ---- GLES2 没有的桌面 GL 函数（做成空实现，防止链接错误）
inline void glGetTexLevelParameteriv(unsigned int, int, unsigned int, int*) {}

// ---- GL 常量（GLES2 头里可能没有）
#ifndef GL_TEXTURE_WIDTH
#define GL_TEXTURE_WIDTH  0x1000
#endif
#ifndef GL_TEXTURE_HEIGHT
#define GL_TEXTURE_HEIGHT 0x1001
#endif

#endif // __ANDROID__
