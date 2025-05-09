#
# CMakeLists.txt
#
# Copyright (C) 2022 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

project (CORE)

add_subdirectory(tex/synctex)

# include files
file(GLOB_RECURSE CORE_HEADER_FILES "*.h*")

# source files
set(CORE_SOURCE_FILES
   Backtrace.cpp
   Base64.cpp
   BoostErrors.cpp
   BrowserUtils.cpp
   collection/MruList.cpp
   ConfigProfile.cpp
   ConfigUtils.cpp
   CrashHandler.cpp
   DateTime.cpp
   ExponentialBackoff.cpp
   Exec.cpp
   FileInfo.cpp
   FileSerializer.cpp
   FileUtils.cpp
   GitGraph.cpp
   HtmlUtils.cpp
   Log.cpp
   LogOptions.cpp
   PerformanceTimer.cpp
   ProgramOptions.cpp
   RegexUtils.cpp
   RecursionGuard.cpp
   Settings.cpp
   SocketRpc.cpp
   StringUtils.cpp
   ColorUtils.cpp
   Thread.cpp
   Timer.cpp
   Trace.cpp
   WaitUtils.cpp
   YamlUtil.cpp
   ZlibUtil.cpp
   file_lock/FileLock.cpp
   file_lock/AdvisoryFileLock.cpp
   file_lock/LinkBasedFileLock.cpp
   gwt/GwtFileHandler.cpp
   gwt/GwtLogHandler.cpp
   gwt/GwtSymbolMaps.cpp
   libclang/CodeCompleteResults.cpp
   libclang/Cursor.cpp
   libclang/Diagnostic.cpp
   libclang/LibClang.cpp
   libclang/SourceIndex.cpp
   libclang/SourceLocation.cpp
   libclang/SourceRange.cpp
   libclang/Token.cpp
   libclang/TranslationUnit.cpp
   libclang/UnsavedFiles.cpp
   libclang/Utils.cpp
   json/JsonRpc.cpp
   http/Cookie.cpp
   http/Header.cpp
   http/Message.cpp
   http/MultipartRelated.cpp
   http/ChunkParser.cpp
   http/ChunkProxy.cpp
   http/CSRFToken.cpp
   http/FormProxy.cpp
   http/ProxyUtils.cpp
   http/NoProxyRules.cpp
   http/Request.cpp
   http/RequestParser.cpp
   http/Response.cpp
   http/SocketProxy.cpp
   http/Ssl.cpp
   http/URL.cpp
   http/UriHandler.cpp
   http/Util.cpp
   markdown/Markdown.cpp
   markdown/MathJax.cpp
   markdown/sundown/autolink.c
   markdown/sundown/buffer.c
   markdown/sundown/houdini_href_e.c
   markdown/sundown/houdini_html_e.c
   markdown/sundown/html.c
   markdown/sundown/html_smartypants.c
   markdown/sundown/markdown.c
   markdown/sundown/stack.c
   r_util/RActiveSessions.cpp
   r_util/RActiveSessionStorage.cpp
   r_util/RActiveSessionsStorage.cpp
   r_util/RPackageInfo.cpp
   r_util/RProjectFile.cpp
   r_util/RSessionContext.cpp
   r_util/RTokenizer.cpp
   r_util/RSourceIndex.cpp
   r_util/RUserData.cpp
   spelling/HunspellCustomDictionaries.cpp
   spelling/HunspellDictionaryManager.cpp
   spelling/HunspellSpellingEngine.cpp
   system/Architecture.cpp
   system/ChildProcessSubprocPoll.cpp
   system/Crypto.cpp
   system/Environment.cpp
   system/Process.cpp
   system/ShellUtils.cpp
   system/System.cpp
   system/Xdg.cpp
   system/file_monitor/FileMonitor.cpp
   system/encryption/Encryption.cpp
   terminal/PrivateCommand.cpp
   tex/TexLogParser.cpp
   tex/TexMagicComment.cpp
   tex/TexSynctex.cpp
   text/AnsiCodeParser.cpp
   text/DcfParser.cpp
   text/TextCursor.cpp
   text/TemplateFilter.cpp
   text/TermBufferParser.cpp
)

if (RSTUDIO_HAS_SOCI)
   list(APPEND CORE_SOURCE_FILES Database.cpp)
endif()

# UNIX specific
if (UNIX)

   include (CheckCXXSourceCompiles)
   CHECK_CXX_SOURCE_COMPILES (
   "# include <dirent.h>
   int func (const char *d, dirent ***list, void *sort)
   {
      int n = scandir(d, list, 0, (int(*)(const dirent **, const dirent **))sort);
      return n;
   }

   int main (int, char **)
   {
      return 0;
   }
   "
   HAVE_SCANDIR_POSIX)

   # platform introspection
   check_symbol_exists(SA_NOCLDWAIT "signal.h" HAVE_SA_NOCLDWAIT)
   check_symbol_exists(SO_PEERCRED "sys/socket.h" HAVE_SO_PEERCRED)
   check_function_exists(inotify_init1 HAVE_INOTIFY_INIT1)
   check_function_exists(getpeereid HAVE_GETPEEREID)
   check_function_exists(setresuid HAVE_SETRESUID)
   if(EXISTS "/proc/self")
      set(HAVE_PROCSELF TRUE)
   endif()

   # missing on non-glibc platforms like macOS, musl-based Linux distros, and
   # the BSDs
   check_function_exists(group_member HAVE_GROUP_MEMBER)

   # missing on musl-based Linux distros and some BSDs
   CHECK_CXX_SOURCE_COMPILES (
   "#include <execinfo.h>
   int main (int, char **)
   {
      return 0;
   }
   "
   HAVE_EXECINFO)

   # find packages and libraries
   find_library(PTHREAD_LIBRARIES pthread)
   if(NOT APPLE)
      find_library(UTIL_LIBRARIES util)
      find_library(UUID_LIBRARIES uuid)
      find_library(RT_LIBRARIES rt)
   endif()
   find_package(ZLIB REQUIRED QUIET)

   # find apple frameworks we depend on
   if(APPLE)
      find_library(CORE_SERVICES_LIBRARY NAMES CoreServices)
   endif()

   # include directories and libraries
   set(CORE_SYSTEM_LIBRARIES
      ${PTHREAD_LIBRARIES}
      ${UTIL_LIBRARIES}
      ${UUID_LIBRARIES}
      ${RT_LIBRARIES}
      ${ZLIB_LIBRARIES}
      ${CORE_SERVICES_LIBRARY})

   list(APPEND CORE_SYSTEM_LIBRARIES ${OPENSSL_LIBRARIES})
   list(APPEND CORE_INCLUDE_DIRS ${OPENSSL_INCLUDE_DIR})

   if(RSTUDIO_SERVER AND RSTUDIO_USE_PAM)
      find_package(PAM REQUIRED)
      list(APPEND CORE_SYSTEM_LIBRARIES ${PAM_LIBRARIES})
      list(APPEND CORE_INCLUDE_DIRS ${PAM_INCLUDE_DIRS})
   endif()

   # source files
   set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
      ${DIRECTORY_MONITOR_CPP}
      PosixStringUtils.cpp
      r_util/REnvironmentPosix.cpp
      r_util/RSessionLaunchProfile.cpp
      r_util/RVersionsPosix.cpp
      system/Container.cpp
      system/PosixChildProcessTracker.cpp
      system/PosixEnvironment.cpp
      system/PosixFileScanner.cpp
      system/PosixInterrupts.cpp
      system/PosixLibraryLoader.cpp
      system/PosixNfs.cpp
      system/PosixParentProcessMonitor.cpp
      system/PosixOutputCapture.cpp
      system/PosixSched.cpp
      system/PosixShellUtils.cpp
      system/PosixSystem.cpp
      system/PosixUser.cpp
      system/PosixGroup.cpp
      system/PosixChildProcess.cpp
      system/PosixProcess.cpp
      system/User.cpp
   )

   if(APPLE)
      set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
         http/Keychain.mm
         system/file_monitor/MacFileMonitor.cpp
         system/recycle_bin/MacRecycleBin.cpp
         system/MacResources.cpp
      )
   else()
      set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
         system/file_monitor/LinuxFileMonitor.cpp
         system/recycle_bin/LinuxRecycleBin.cpp
         system/LinuxResources.cpp
      )
   endif()

# Win32 specific
else()

   # system libraries
   set(CORE_SYSTEM_LIBRARIES
      WS2_32
      MsWSock
      RpcRT4
      ShLwApi
      AdvAPI32
      Crypt32
      ${OPENSSL_LIBRARIES}
   )

   # source files
   set(CORE_SOURCE_FILES ${CORE_SOURCE_FILES}
      http/NamedPipeProtocol.cpp
      system/Win32FileScanner.cpp
      system/RegistryKey.cpp
      system/Win32Environment.cpp
      system/Win32Interrupts.cpp
      system/Win32LibraryLoader.cpp
      system/Win32ParentProcessMonitor.cpp
      system/Win32Pty.cpp
      system/Win32OutputCapture.cpp
      system/Win32RuntimeLibrary.cpp
      system/Win32ShellUtils.cpp
      system/Win32System.cpp
      system/Win32ChildProcess.cpp
      system/Win32Resources.cpp
      system/file_monitor/Win32FileMonitor.cpp
      system/recycle_bin/Win32RecycleBin.cpp
      r_util/RToolsInfo.cpp
   )

endif()

configure_file(
   ${CMAKE_CURRENT_SOURCE_DIR}/config.h.in
   ${CMAKE_CURRENT_BINARY_DIR}/config.h)

# define include dirs
list(APPEND CORE_INCLUDE_DIRS include)

# search for core addins
if(RSTUDIO_ADDINS_PATH)
   set(CORE_ADDIN_PATH ${RSTUDIO_ADDINS_PATH}/core)
   if(EXISTS ${CORE_ADDIN_PATH})
      file(GLOB_RECURSE ADDIN_HEADER_FILES "${CORE_ADDIN_PATH}/*.h*")
      list(APPEND CORE_HEADER_FILES ${ADDIN_HEADER_FILES})
      file(GLOB_RECURSE ADDIN_SOURCE_FILES "${CORE_ADDIN_PATH}/*.c*")
      list(APPEND CORE_SOURCE_FILES ${ADDIN_SOURCE_FILES})
      list(APPEND CORE_INCLUDE_DIRS ${CORE_ADDIN_PATH}/include)
   endif()
endif()

set(SHARED_CORE_INCLUDE_DIRS
   ${SHARED_CORE_SOURCE_DIR}/include)

include_directories(
   ${CORE_INCLUDE_DIRS}
   ${SHARED_CORE_INCLUDE_DIRS}
   ${EXT_SOURCE_DIR}
   ${CMAKE_CURRENT_BINARY_DIR}
   ${TESTS_INCLUDE_DIR}
)

# define shared library
add_library(rstudio-core STATIC ${CORE_SOURCE_FILES} ${CORE_HEADER_FILES})
define_source_file_names(rstudio-core)

# link dependencies
target_link_libraries(rstudio-core
   rstudio-shared-core
   rstudio-hunspell
   rstudio-websocketpp
   ${Boost_LIBRARIES}
   ${CRASHPAD_LIBRARIES}
   ${SOCI_LIBRARIES}
   ${CORE_SYSTEM_LIBRARIES}
   fmt::fmt
   gsl::gsl-lite-v1
   tl::expected
   yaml-cpp::yaml-cpp
)

if(WIN32)
   target_link_libraries(rstudio-core zlibstatic)
endif()

# define executable (for running unit tests)
if (RSTUDIO_UNIT_TESTS_ENABLED)

   file(GLOB_RECURSE CORE_TEST_FILES "*Tests.cpp")
   if (NOT RSTUDIO_HAS_SOCI)
      list(REMOVE_ITEM CORE_TEST_FILES "${CMAKE_CURRENT_SOURCE_DIR}/DatabaseTests.cpp")
   endif()

   add_executable(rstudio-core-tests
      TestMain.cpp
      ${CORE_TEST_FILES}
      ${CORE_HEADER_FILES}
   )

   target_link_libraries(rstudio-core-tests
      rstudio-shared-core
      rstudio-core
      rstudio-core-synctex
      rstudio-hunspell
      ${Boost_LIBRARIES}
      ${SOCI_LIBRARIES}
      ${CORE_SYSTEM_LIBRARIES}
   )

   if(WIN32)
      target_link_libraries(rstudio-core-tests zlibstatic)
   endif()

endif()
