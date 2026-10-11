// CppSekai - GLES3 runtime loader for functions libGLESv2.so doesn't export.
// Android's libGLESv2.so only exports GLES2 entry points; GLES3 functions
// (VAO, sampler) must be fetched via eglGetProcAddress / SDL_GL_GetProcAddress.
#ifdef __ANDROID__
#include <GLES3/gl3.h>
#include <SDL.h>

extern "C" {

typedef void (*PFN_glGenVertexArrays)(GLsizei, GLuint*);
typedef void (*PFN_glBindVertexArray)(GLuint);
typedef void (*PFN_glDeleteVertexArrays)(GLsizei, const GLuint*);
typedef void (*PFN_glBindSampler)(GLuint, GLuint);
typedef void (*PFN_glGenSamplers)(GLsizei, GLuint*);
typedef void (*PFN_glDeleteSamplers)(GLsizei, const GLuint*);
typedef void (*PFN_glSamplerParameteri)(GLuint, GLenum, GLint);

static PFN_glGenVertexArrays     p_glGenVertexArrays = nullptr;
static PFN_glBindVertexArray     p_glBindVertexArray = nullptr;
static PFN_glDeleteVertexArrays  p_glDeleteVertexArrays = nullptr;
static PFN_glBindSampler         p_glBindSampler = nullptr;
static PFN_glGenSamplers         p_glGenSamplers = nullptr;
static PFN_glDeleteSamplers      p_glDeleteSamplers = nullptr;
static PFN_glSamplerParameteri   p_glSamplerParameteri = nullptr;

static void* getProc(const char* name)
{
    return (void*)SDL_GL_GetProcAddress(name);
}

void glGenVertexArrays(GLsizei n, GLuint* arrays)
{
    if (!p_glGenVertexArrays) p_glGenVertexArrays = (PFN_glGenVertexArrays)getProc("glGenVertexArrays");
    if (p_glGenVertexArrays) p_glGenVertexArrays(n, arrays);
}

void glBindVertexArray(GLuint array)
{
    if (!p_glBindVertexArray) p_glBindVertexArray = (PFN_glBindVertexArray)getProc("glBindVertexArray");
    if (p_glBindVertexArray) p_glBindVertexArray(array);
}

void glDeleteVertexArrays(GLsizei n, const GLuint* arrays)
{
    if (!p_glDeleteVertexArrays) p_glDeleteVertexArrays = (PFN_glDeleteVertexArrays)getProc("glDeleteVertexArrays");
    if (p_glDeleteVertexArrays) p_glDeleteVertexArrays(n, arrays);
}

void glBindSampler(GLuint unit, GLuint sampler)
{
    if (!p_glBindSampler) p_glBindSampler = (PFN_glBindSampler)getProc("glBindSampler");
    if (p_glBindSampler) p_glBindSampler(unit, sampler);
}

void glGenSamplers(GLsizei n, GLuint* s)
{
    if (!p_glGenSamplers) p_glGenSamplers = (PFN_glGenSamplers)getProc("glGenSamplers");
    if (p_glGenSamplers) p_glGenSamplers(n, s);
}

void glDeleteSamplers(GLsizei n, const GLuint* s)
{
    if (!p_glDeleteSamplers) p_glDeleteSamplers = (PFN_glDeleteSamplers)getProc("glDeleteSamplers");
    if (p_glDeleteSamplers) p_glDeleteSamplers(n, s);
}

void glSamplerParameteri(GLuint s, GLenum pname, GLint param)
{
    if (!p_glSamplerParameteri) p_glSamplerParameteri = (PFN_glSamplerParameteri)getProc("glSamplerParameteri");
    if (p_glSamplerParameteri) p_glSamplerParameteri(s, pname, param);
}

} // extern "C"
#endif
