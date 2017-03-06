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

__author__ = 'mhall'

import sys
import socket
import struct
import os
import json
import base64
import math
import traceback
import pandas as pd
import matplotlib.pyplot as plt

_global_python3 = sys.version_info >= (3, 0)

if _global_python3:
    from io import StringIO
    from io import BytesIO
else:
    try:
        from cStringIO import StringIO
    except:
        from StringIO import StringIO

try:
    import cPickle as pickle
except:
    import pickle

_global_connection = None
_global_env = {}
# _local_env = {}
# _headers = {}

_global_startup_debug = False

# _global_std_out = StringIO()
# _global_std_err = StringIO()
sys.stdout = StringIO()
sys.stderr = StringIO()

if len(sys.argv) > 2:
    if sys.argv[2] == 'debug':
        _global_startup_debug = True


def runServer():
    if _global_startup_debug == True:
        print('Python server starting...\n')
    # _local_env['headers'] = {}
    # _local_env['frames'] = {}
    global _global_connection
    _global_connection = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    _global_connection.connect(('localhost', int(sys.argv[1])))
    pid_response = {}
    pid_response['response'] = 'pid_response'
    pid_response['pid'] = os.getpid()
    send_response(pid_response, True)
    try:
        while 1:
            message = receive_message(True)
            if 'command' in message:
                command = message['command']
                if command == 'put_instances':
                    receive_instances(message)
                elif command == 'get_instances':
                    send_instances(message)
                elif command == 'execute_script':
                    execute_script(message)
                elif command == 'get_variable_list':
                    send_variable_list(message)
                elif command == 'get_variable_type':
                    send_variable_type(message)
                elif command == 'get_variable_value':
                    send_variable_value(message)
                elif command == 'get_image':
                    send_image_as_png(message)
                elif command == 'variable_is_set':
                    send_variable_is_set(message)
                elif command == 'set_variable_value':
                    receive_variable_value(message)
                elif command == 'get_debug_buffer':
                    send_debug_buffer()
                elif command == 'shutdown':
                    if _global_startup_debug == True:
                        print ('Received shutdown command...\n')
                    exit()
            else:
                if _global_startup_debug == True:
                    print('message did not contain a command field!')
    finally:
        _global_connection.close()


def message_debug(message):
    if 'debug' in message:
        return message['debug']
    else:
        return False


def send_debug_buffer():
    tOut = sys.stdout
    tErr = sys.stderr
    ok_response = {}
    ok_response['response'] = 'ok'
    ok_response['std_out'] = tOut.getvalue()
    ok_response['std_err'] = tErr.getvalue()
    # clear the buffers
    tOut.close()
    tErr.close()
    sys.stdout = StringIO()
    sys.stderr = StringIO()
    send_response(ok_response, True)


def receive_instances(message):
    if 'header' in message:
        # get the frame name
        header = message['header']
        frame_name = header['frame_name']
        # could store the header (but don't currently)
        # _headers[frame_name] = header
        num_instances = message['num_instances']
        if num_instances > 0:
            # receive the CSV
            csv_data = receive_message(False)
            frame = None
            if 'date_atts' in header:
                frame = pd.read_csv(StringIO(csv_data), na_values='?',
                                    quotechar='\'', escapechar='\\',
                                    index_col=None,
                                    parse_dates=header['date_atts'],
                                    infer_datetime_format=True)
            else:
                frame = pd.read_csv(StringIO(csv_data), na_values='?',
                                    quotechar='\'', escapechar='\\',
                                    index_col=None)
            _global_env[frame_name] = frame
            if message_debug(message) == True:
                print(frame.info(), '\n')
        ack_command_ok()
    else:
        error = 'put instances json message does not contain a header entry!'
        ack_command_err(error)


def send_instances(message):
    frame_name = message['frame_name']
    frame = get_variable(frame_name)
    if type(frame) is not pd.DataFrame:
        message = 'Variable ' + frame_name
        if frame is None:
            message += ' is not defined'
        else:
            message += ' is not a DataFrame object'
        ack_command_err(message)
    else:
        ack_command_ok()
        # now convert and send data
    response = {}
    response['response'] = 'instances_header'
    response['num_instances'] = len(frame.index)
    response['header'] = instances_to_header_message(frame_name, frame)
    if message_debug(message) == True:
        print(response)
    send_response(response, True)
    # now send the CSV data
    s = StringIO()
    frame.to_csv(path_or_buf=s, na_rep='?', doublequote=False, index=False,
                 quotechar='\'',
                 escapechar='\\', header=False, date_format='"%Y-%m-%d %H:%M:%S"')
    send_response(s.getvalue(), False)


def instances_to_header_message(frame_name, frame):
    num_rows = len(frame.index)
    header = {}
    header['relation_name'] = frame_name
    header['attributes'] = []
    for att_name in frame.dtypes.index:
        attribute = {}
        attribute['name'] = str(att_name)
        type = frame.dtypes[att_name]
        if type == 'object' or type == 'bool':
            # TODO - how to determine nominal?
            attribute['type'] = 'STRING'
            distinct = frame[att_name].unique()
            if distinct.size < num_rows / 2:
                # make it nominal
                attribute['type'] = 'NOMINAL'
                nom_vals = []
                for val in distinct:
                    if not is_nan(val):
                        nom_vals.append(val)
                attribute['values'] = nom_vals
        elif str(type).startswith('datetime'):
            attribute['type'] = "DATE"
            attribute['format'] = 'yyyy-MM-dd HH:mm:ss'
        else:
            attribute['type'] = 'NUMERIC'
        header['attributes'].append(attribute)
    return header


def send_response(response, isJson):
    if isJson is True:
        response = json.dumps(response)

    if _global_python3 is True:
        _global_connection.sendall(struct.pack('>L', len(response)))
        _global_connection.sendall(response.encode('utf-8'))
    else:
        _global_connection.sendall(struct.pack('>L', len(response)))
        _global_connection.sendall(response)


def receive_message(isJson):
    size = 0
    length = None
    if _global_python3 is True:
        length = bytearray()
    else:
        length = ''
    while len(length) < 4:
        if _global_python3 is True:
            length += _global_connection.recv(4);
        else:
            length += _global_connection.recv(4);

    size = struct.unpack('>L', length)[0]

    data = ''
    while len(data) < size:
        if _global_python3 is True:
            data += _global_connection.recv(size).decode('utf-8');
        else:
            data += _global_connection.recv(size);
    if isJson is True:
        return json.loads(data)
    return data


def ack_command_err(message):
    err_response = {}
    err_response['response'] = 'error'
    err_response['error_message'] = message
    send_response(err_response, True)


def ack_command_ok():
    ok_response = {}
    ok_response['response'] = 'ok'
    send_response(ok_response, True)


def get_variable(var_name):
    if var_name in _global_env:
        return _global_env[var_name]
    else:
        return None


def execute_script(message):
    if 'script' in message:
        script = message['script']
        tOut = sys.stdout
        tErr = sys.stderr
        output = StringIO()
        error = StringIO()
        if message_debug(message):
            print('Executing script...\n\n' + script)
        sys.stdout = output
        sys.stderr = error
        try:
            exec (script, _global_env)
        except Exception:
            print('Got an exception executing script')
            traceback.print_exc(file=error)
        sys.stdout = tOut
        sys.stderr = tErr
        # sys.stdout = sys.__stdout__
        # sys.stderr = sys.__stderr__
        ok_response = {}
        ok_response['response'] = 'ok'
        ok_response['script_out'] = output.getvalue()
        ok_response['script_error'] = error.getvalue()
        send_response(ok_response, True)
    else:
        error = 'execute script json message does not contain a script entry!'
        ack_command_err(error)


def send_variable_is_set(message):
    if 'variable_name' in message:
        var_name = message['variable_name']
        var_value = get_variable(var_name)
        ok_response = {}
        ok_response['response'] = 'ok'
        ok_response['variable_name'] = var_name
        if var_value is not None:
            ok_response['variable_exists'] = True
        else:
            ok_response['variable_exists'] = False
        send_response(ok_response, True)
    else:
        error = 'object exists json message does not contain a variable_name entry!'
        ack_command_err(error)


def send_variable_type(message):
    if 'variable_name' in message:
        var_name = message['variable_name']
        var_value = get_variable(var_name)
        if var_value is None:
            ack_command_err('variable ' + var_name + ' is not set!')
        else:
            ok_response = {}
            ok_response['response'] = 'ok'
            ok_response['variable_name'] = var_name
            ok_response['type'] = 'unknown'
            if type(var_value) is pd.DataFrame:
                ok_response['type'] = 'dataframe'
            elif type(var_value) is plt.Figure:
                ok_response['type'] = 'image'
            send_response(ok_response, True)
    else:
        ack_command_err(
            'send variable type json message does not contain a variable_name entry!')


def send_variable_value(message):
    if 'variable_encoding' in message:
        encoding = message['variable_encoding']
        if encoding == 'pickled' or encoding == 'json' or encoding == 'string':
            send_encoded_variable_value(message)
        else:
            ack_command_err(
                'Unknown encoding type for send variable value message')
    else:
        ack_command_err('send variable value message does not contain an '
                        'encoding field')


def send_variable_list(message):
    variables = []
    for key, value in dict(_global_env).items():
        variable_type = type(value).__name__
        if not (
                            variable_type == 'classob' or variable_type == 'module' or variable_type == 'function'):
            variables.append({'name': key, 'type': variable_type})
    ok_response = {}
    ok_response['response'] = 'ok'
    ok_response['variable_list'] = variables
    send_response(ok_response, True)


def base64_encode(value):
    # encode to base 64 bytes
    b64 = base64.b64encode(value)
    # get it as a string
    b64s = b64
    if _global_python3 is True:
        b64s = b64.decode('utf8')
    return b64s


def base64_decode(value):
    b64b = value
    if _global_python3 is True:
        # from string to bytes
        b64b = value.encode()
    # back to non-base64 bytes
    bytes = base64.b64decode(b64b)
    return bytes


def image_as_encoded_string(value):
    # return image as png data encoded in a string.
    # assumes image is a matplotlib.figure.Figure
    encoded = None
    if _global_python3:
        sio = BytesIO()
        value.savefig(sio, format='png')
        encoded = base64_encode(sio.getvalue())
    else:
        sio = StringIO()
        value.savefig(sio, format='png')
        encoded = base64_encode(sio.getvalue())
    return encoded


def send_image_as_png(message):
    if 'variable_name' in message:
        var_name = message['variable_name']
        image = get_variable(var_name)
        if image is not None:
            if type(image) is plt.Figure:
                ok_response = {}
                ok_response['response'] = 'ok'
                ok_response['variable_name'] = var_name
                # encoding = 'plain'
                # if _global_python3 is True:
                encoding = 'base64'
                ok_response['encoding'] = encoding
                ok_response['image_data'] = image_as_encoded_string(image)
                if message_debug(message) == True:
                    print(
                        'Sending ' + var_name + ' base64 encoded as png bytes')
                send_response(ok_response, True)
            else:
                ack_command_err(
                    var_name + ' is not a matplot.figure.Figure object')
        else:
            ack_command_err(var_name + ' does not exist!')
    else:
        ack_command_err(
            'get image json message does not contain a variable_name entry!')


def send_encoded_variable_value(message):
    if 'variable_name' in message:
        var_name = message['variable_name']
        object = get_variable(var_name)
        if object is not None:
            encoding = message['variable_encoding']
            encoded_object = None
            if encoding == 'pickled':
                encoded_object = pickle.dumps(object)
                if _global_python3 is True:
                    encoded_object = base64_encode(encoded_object)
            elif encoding == 'json':
                encoded_object = object  # the whole response gets serialized to json
            elif encoding == 'string':
                encoded_object = str(object)
            ok_response = {}
            ok_response['response'] = 'ok'
            ok_response['variable_name'] = var_name
            ok_response['variable_encoding'] = encoding
            ok_response['variable_value'] = encoded_object
            if message_debug(message) == True:
                print(
                    'Sending ' + encoding + ' value for var ' + var_name + "\n")

            send_response(ok_response, True)
        else:
            ack_command_err(var_name + ' does not exist!')
    else:
        ack_command_err(
            'get variable value json message does not contain a variable_name entry!')


def receive_variable_value(message):
    if 'variable_encoding' in message:
        if message['variable_encoding'] == 'pickled':
            receive_pickled_variable_value(message)
    else:
        ack_command_err('receive variable value message does not contain an '
                        'encoding field')


def receive_pickled_variable_value(message):
    if 'variable_name' in message and 'variable_value' in message:
        var_name = message['variable_name']

        pickled_var_value = message['variable_value']
        # print("Just before de-pickling")
        # print(pickled_var_value)
        if _global_python3:
            pickled_var_value = base64_decode(pickled_var_value)
        else:
            pickled_var_value = str(pickled_var_value)
        object = pickle.loads(pickled_var_value)
        _global_env[var_name] = object
        ack_command_ok()
    else:
        error = 'put variable value json message does not contain a ' \
                'variable_name or variable_value entry!'
        ack_command_err(error)


def is_nan(s):
    try:
        return math.isnan(s)
    except:
        return False


runServer()
