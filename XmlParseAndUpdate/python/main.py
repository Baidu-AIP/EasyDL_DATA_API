# coding=utf-8
import xml.dom.minidom as dom
import os
import sys
import json
import base64

# 保证兼容python2以及python3
IS_PY3 = sys.version_info.major == 3
if IS_PY3:
    from urllib.request import urlopen
    from urllib.request import Request
    from urllib.error import URLError
    from urllib.parse import urlencode
    from urllib.parse import quote_plus
else:
    import urllib2
    from urllib import quote_plus
    from urllib2 import urlopen
    from urllib2 import Request
    from urllib2 import URLError
    from urllib import urlencode

# 防止https证书校验不正确
import ssl
ssl._create_default_https_context = ssl._create_unverified_context

API_KEY = '17wfWy3Fx4g3tKnMAtED7iWG'

SECRET_KEY = 'cmbdyWn3ophc3k2ogLeGsnnGh8AmGOXU'


ADD_URL = "https://aip.baidubce.com/rpc/2.0/easydl/dataset/addentity"


"""  TOKEN start """
TOKEN_URL = 'https://aip.baidubce.com/oauth/2.0/token'


"""
    获取token
"""
def fetch_token():
    params = {'grant_type': 'client_credentials',
              'client_id': API_KEY,
              'client_secret': SECRET_KEY}
    post_data = urlencode(params)
    if (IS_PY3):
        post_data = post_data.encode('utf-8')
    req = Request(TOKEN_URL, post_data)
    try:
        f = urlopen(req, timeout=5)
        result_str = f.read()
    except URLError as err:
        print(err)
    if (IS_PY3):
        result_str = result_str.decode()


    result = json.loads(result_str)

    if ('access_token' in result.keys() and 'scope' in result.keys()):
        if not 'brain_all_scope' in result['scope'].split(' '):
            print ('please ensure has check the  ability')
            exit()
        return result['access_token']
    else:
        print ('please overwrite the correct API_KEY and SECRET_KEY')
        exit()

"""
    读取文件
"""
def read_file(image_path):
    f = None
    try:
        f = open(image_path, 'rb')
        return f.read()
    except:
        print('read image file fail')
        return None
    finally:
        if f:
            f.close()

"""
    调用远程服务
"""
def request(url, data):
    req = Request(url, data.encode('utf-8'))
    has_error = False
    try:
        f = urlopen(req)
        result_str = f.read()
        if (IS_PY3):
            result_str = result_str.decode()
        return result_str
    except  URLError as err:
        print(err)

"""
    单个文件解析并请求添加数据API
    参考https://ai.baidu.com/docs#/EasyDL_DATA_API/0e4e34d9
"""
def send(url, filename):
    dom_object = dom.parse(filename)
    doc_el = dom_object.documentElement

    folder = doc_el.getElementsByTagName("folder")[0].childNodes[0].data
    filename = doc_el.getElementsByTagName("filename")[0].childNodes[0].data

    filepath = folder + "/" + filename
    labels = []
    base64_content = base64.b64encode(read_file(filepath))

    for obj in doc_el.getElementsByTagName("object"):
        name = obj.getElementsByTagName("name")[0].childNodes[0].data
        xmin = int(obj.getElementsByTagName("xmin")[0].childNodes[0].data)
        xmax = int(obj.getElementsByTagName("xmax")[0].childNodes[0].data)
        ymin = int(obj.getElementsByTagName("ymin")[0].childNodes[0].data)
        ymax = int(obj.getElementsByTagName("ymax")[0].childNodes[0].data)

        labels.append({
            "label_name": name,
            "left": xmin,
            "top": ymin,
            "width": (xmax - xmin),
            "height": (ymax - ymin)
            })

    params = json.dumps({
        'type': 'OBJECT_DETECTION',
        'dataset_id': 44820,
        'entity_name': filename,
        'entity_content': base64_content,
        'labels': labels
    })

    # 调用数据上传接口
    result = request(url, params)

    # 打印结果
    print(result)

if __name__ == '__main__':

    # 获取access token
    token = fetch_token()

    # 拼接带token的url
    url = ADD_URL + "?access_token=" + token

    # 单个文件解析并请求添加数据API
    send(url, '../voc.xml')
