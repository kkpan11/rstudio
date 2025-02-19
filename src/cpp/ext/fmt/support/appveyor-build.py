#!/usr/bin/env python
# Build the project on AppVeyor.

import os
from subprocess import check_call

build = os.environ['BUILD']
config = os.environ['CONFIGURATION']
platform = os.environ['PLATFORM']
path = os.environ['PATH']
image = os.environ['APPVEYOR_BUILD_WORKER_IMAGE']
jobid = os.environ['APPVEYOR_JOB_ID']
cmake_command = ['cmake', '-DFMT_PEDANTIC=ON', '-DCMAKE_BUILD_TYPE=' + config, '..']
if build == 'mingw':
    cmake_command.append('-GMinGW Makefiles')
    build_command = ['mingw32-make', '-j4']
    test_command = ['mingw32-make', 'test']
    # Remove the path to Git bin directory from $PATH because it breaks
    # MinGW config.
    path = path.replace(r'C:\Program Files (x86)\Git\bin', '')
    os.environ['PATH'] = r'C:\MinGW\bin;' + path
else:
    # Add MSBuild 14.0 to PATH as described in
    # http://help.appveyor.com/discussions/problems/2229-v140-not-found-on-vs2105rc.
    os.environ['PATH'] = r'C:\Program Files (x86)\MSBuild\15.0\Bin;' + path
    if image == 'Visual Studio 2022':
        generator = 'Visual Studio 17 2022'
        if platform == 'x64':
            cmake_command.extend(['-A', 'x64'])
    elif image == 'Visual Studio 2019':
        generator = 'Visual Studio 16 2019'
        if platform == 'x64':
            cmake_command.extend(['-A', 'x64'])
    else:
        if image == 'Visual Studio 2015':
            generator = 'Visual Studio 14 2015'
        elif image == 'Visual Studio 2017':
            generator = 'Visual Studio 15 2017'
        if platform == 'x64':
            generator += ' Win64'
    cmake_command.append('-G' + generator)
    build_command = ['cmake', '--build', '.', '--config', config, '--', '/m:4']
    test_command = ['ctest', '-C', config]

check_call(cmake_command)
check_call(build_command)
check_call(test_command)
