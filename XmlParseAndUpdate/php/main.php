<?php
    define('DEMO_CURL_VERBOSE', false); // 打印curl debug信息
    /**
     * php -m 检查是否开启php curl扩展
     */
    # 填写网页上申请的appkey 如 $apiKey="g8eBUMSokVB1BHGmgxxxxxx"
    $API_KEY =  "xxx";
    # 填写网页上申请的APP SECRET 如 $secretKey="94dc99566550d87f8fa8ece112xxxxx"
    $SECRET_KEY = "xxx";

    $ADD_URL = "https://aip.baidubce.com/rpc/2.0/easydl/dataset/addentity";

    /** 公共模块获取token开始 */
    $auth_url = "http://openapi.baidu.com/oauth/2.0/token?grant_type=client_credentials&client_id=".$API_KEY."&client_secret=" . $SECRET_KEY;

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $auth_url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false); //信任任何证书
    curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0); // 检查证书中是否设置域名,0不验证
    curl_setopt($ch, CURLOPT_VERBOSE, DEMO_CURL_VERBOSE);
    $res = curl_exec($ch);
    if(curl_errno($ch))
    {
        print curl_error($ch);
    }
    curl_close($ch);

    $response = json_decode($res, true);

    $token = $response['access_token'];

    $ADD_URL .= "?access_token=" . $token;

    send($ADD_URL, "../voc.xml");

    function send($url, $filepath)
    {

        $file_content = file_get_contents($filepath);
        $dom = new SimpleXMLElement($file_content);
        $folder = $dom->folder;
        $filename = $dom->filename;
        $filepath = $folder . "/" . $filename;
        $labels = array();

        $entity_content = base64_encode(file_get_contents($filepath));

        foreach ($dom->object as $obj) {
            $name = $obj->name;
            $xmin = $obj->bndbox->xmin;
            $xmax = $obj->bndbox->xmax;
            $ymin = $obj->bndbox->ymin;
            $ymax = $obj->bndbox->ymax;
            $labels[] = array(
                'label_name' => $name . "",
                'left' => intval($xmin),
                'top' => intval($ymin),
                'width' => $xmax - $xmin,
                'height' => $ymax - $ymin
            );
        }

        $params = array(
            'type' => 'OBJECT_DETECTION',
            'dataset_id' => 44820,
            'entity_name' => $filename . "",
            'entity_content' => $entity_content,
            'labels' => $labels
        );

        

        $json_array = json_encode($params);

        $headers[] = "Content-Length: ".strlen($json_array);
        $headers[] = 'Content-Type: application/json; charset=utf-8';

        /** asr 请求开始 **/
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
        curl_setopt($ch, CURLOPT_TIMEOUT, 10);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $json_array);
        curl_setopt($ch, CURLOPT_VERBOSE, DEMO_CURL_VERBOSE);

        $res = curl_exec($ch);

        if(curl_errno($ch))
        {
            echo curl_error($ch);
            exit (2);
        }
        curl_close($ch);

        echo $res;
    }

    ?>
