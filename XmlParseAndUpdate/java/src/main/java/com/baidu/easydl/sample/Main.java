package com.baidu.easydl.sample;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;

public class Main {

    static Document doc;

    static final String ADD_URL = "https://aip.baidubce.com/rpc/2.0/easydl/dataset/addentity";

    public static void main(String[] args) {

        String xmlFilePath = "../voc.xml";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            JSONObject jo = new JSONObject();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(new File(xmlFilePath));
            String folder = getNodeText("folder");
            String filename = getNodeText("filename");
            String filePath = folder + File.separator + filename;
            byte[] fileContent = readFileContent(filePath);

            jo.put("type", "OBJECT_DETECTION");
            jo.put("dataset_id", 44820);
            jo.put("entity_name", filename);
            byte[] base64Image = Base64.getEncoder().encode(fileContent);
            String base64ImageStr = new String(base64Image);
            jo.put("entity_content", base64ImageStr);

            NodeList nl = doc.getElementsByTagName("object");
            JSONArray ja = new JSONArray();

            for (int i = 0; i < nl.getLength(); i++) {
                Node nd = nl.item(i);
                NodeList snl = nd.getChildNodes();
                JSONObject labelJo = new JSONObject();
                for (int j = 0; j < snl.getLength(); j++) {

                    if (snl.item(j).getNodeName().equals("name")) {
                        String name = snl.item(j).getFirstChild().getNodeValue();
                        labelJo.put("label_name", name);
                    }
                    if (snl.item(j).getNodeName().equals("bndbox")) {
                        NodeList ssnl =  snl.item(j).getChildNodes();
                        int left = 0;
                        int top = 0;
                        int xmax = 0;
                        int ymax = 0;
                        int height;
                        int width;
                        for (int k = 0; k < ssnl.getLength(); k++) {
                            if (ssnl.item(k).getNodeName().equals("xmin")) {
                                left = Integer.parseInt(ssnl.item(k).getFirstChild().getNodeValue());
                            }
                            if (ssnl.item(k).getNodeName().equals("xmax")) {
                                xmax = Integer.parseInt(ssnl.item(k).getFirstChild().getNodeValue());
                            }
                            if (ssnl.item(k).getNodeName().equals("ymin")) {
                                top = Integer.parseInt(ssnl.item(k).getFirstChild().getNodeValue());
                            }
                            if (ssnl.item(k).getNodeName().equals("ymax")) {
                                ymax = Integer.parseInt(ssnl.item(k).getFirstChild().getNodeValue());
                            }
                        }
                        height = ymax - top;
                        width = xmax - left;
                        labelJo.put("left", left);
                        labelJo.put("top", top);
                        labelJo.put("width", width);
                        labelJo.put("height", height);
                        ja.put(labelJo);
                    }
                }
            }

            jo.put("labels", ja);

            String token = AuthService.getAuth();
            String response = post(ADD_URL + "?access_token=" + token, jo.toString());
            System.out.println(response);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送http请求
     *
     * @param body   http请求体
     * @param urlStr http请求url
     * @return http响应数据字符串
     */
    private static String post(String urlStr, String body) throws IOException {

        URL url = new URL(urlStr);

        // 创建连接
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestMethod("POST");


        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        con.setDoOutput(true);

        OutputStream out = con.getOutputStream();

        // 写入请求数据
        out.write(body.getBytes());
        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer sb = new StringBuffer();
        char[] cs = new char[512];
        int read;
        while ((read = br.read(cs)) != -1) {
            sb.append(new String(cs, 0, read));
        }
        br.close();

        String result = sb.toString();;
        return result;
    }


    private static byte[] readFileContent(String fileName) {
        try {
            return Files.readAllBytes(new File(fileName).toPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getNodeText(String tagName) {
        return doc.getElementsByTagName(tagName).item(0).getTextContent();
    }
}
