##
##   This program is free software: you can redistribute it and/or modify
##   it under the terms of the GNU General Public License as published by
##   the Free Software Foundation, either version 3 of the License, or
##   (at your option) any later version.
##
##   This program is distributed in the hope that it will be useful,
##   but WITHOUT ANY WARRANTY; without even the implied warranty of
##   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##   GNU General Public License for more details.
##
##   You should have received a copy of the GNU General Public License
##   along with this program.  If not, see <http://www.gnu.org/licenses/>.
##

import sys

python_version_min = '2.7.0'
pandas_version_min = '0.7.0'

global_results = ''

def main():
    pyVersion = sys.version_info
    # print('python version: ' + str(pyVersion[0]) + '.' + str(
    #    pyVersion[1]) + '.' + str(pyVersion[2]))
    check_libraries()
    print(global_results)


def check_libraries():
    check_min_python()
    isPython3 = sys.version_info >= (3, 0)
    if isPython3:
        check_library('io')
    else:
        check_library('StringIO')
    check_library('math')
    check_library('traceback')
    check_library('socket')
    check_library('struct')
    check_library('os')
    check_library('json')
    check_library('base64')
    check_library('pickle')
    check_library('scipy')
    check_library('sklearn')
    check_library('matplotlib')

    check_library('numpy')
    if check_library('pandas', ['DataFrame'], pandas_version_min):
        check_min_pandas()


def check_min_python():
    base = python_version_min.split('.')
    pyVersion = sys.version_info
    result = check_min_version(base, sys.version_info)
    if result:
        append_to_results('Installed python does not meet min requirement')


def check_min_pandas():
    min_pandas = pandas_version_min.split('.')
    try:
        import pandas

        actual_pandas = pandas.__version__.split('.')
        # Some versions of pandas (i.e included in the Intel Python distro)
        # have version numbers with more than 3 parts (e.g. 0.22.0+0.ga00154d.dirty).
        # So, this check is now commented out
        # if len(actual_pandas) is not len(min_pandas):
        #    raise Exception()
        #if len(actual_pandas) is not len(min_pandas):
        #    raise Exception()
        result = check_min_version(min_pandas, actual_pandas)
        if result:
            append_to_results(
                'Installed pandas does not meet the minimum requirement: version ' + pandas_version_min)
    except:
        append_to_results('A problem occurred when trying to import pandas')


def check_min_version(base, actual):
    ok = False
    equal = False
    for i in range(len(base)):
        if int(actual[i]) > int(base[i]):
            ok = True
            equal = False
            break;
        if not ok and int(actual[i]) < int(base[i]):
            equal = False
            break;
        if int(actual[i] == int(base[i])):
            equal = True

    return not ok and not equal


def check_library(library, cls=[], version=None):
    ok = True
    if not check_library_available(library):
        ok = False
        result = 'Library "' + library + '" is not available'
        if version is not None:
            result += ', minimum version = ' + version
        append_to_results(result)
    else:
        for c in cls:
            if not is_class_available(library, c):
                ok = False
                append_to_results(
                    'Required class ' + c + ' in library ' + library + ' is not available')
    return ok


def is_class_available(library, cls):
    env = {}
    exec (
        'try:\n\tfrom ' + library + ' import ' + cls + '\n\tresult = True\nexcept:\n\tresult = False',
        {}, env)
    return env['result']


def check_library_available(library):
    env = {}
    exec (
        'try:\n\timport ' + library + '\n\tresult = True\nexcept:\n\tresult = False',
        {}, env)
    return env['result']


def append_to_results(line):
    global global_results
    global_results += line + '\n'


main()
