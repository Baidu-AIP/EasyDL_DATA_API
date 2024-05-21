#include <string>
#include <fstream>
#include <sstream>
#include "tinyxml2.h"
#include "json.hpp"
#include "base64.h"
#include "curl/curl.h"


using namespace tinyxml2;
using namespace nlohmann;

#define TOKEN_URL "https://aip.baidubce.com/oauth/2.0/token"
#define API_URL "https://aip.baidubce.com/rpc/2.0/easydl/dataset/addentity"

#define AK "xxx"
#define SK "xxx"

template<class CharT, class Traits, class Allocator>
std::basic_istream<CharT, Traits>& getall(std::basic_istream<CharT, Traits>& input,
                                          std::basic_string<CharT, Traits, Allocator>& str) {
    std::ostringstream oss;
    oss << input.rdbuf();
    str.assign(oss.str());
    return input;
}

int get_file_content(const char *filename, std::string* out) {
    std::ifstream in(filename, std::ios::in | std::ios::binary);
    if (in) {
        getall(in, *out);
        return 0;
    } else {
        return -1;
    }
}

inline size_t onWriteData(void * buffer, size_t size, size_t nmemb, void * userp)
{
    std::string * str = dynamic_cast<std::string *>((std::string *)userp);
    str->append((char *)buffer, size * nmemb);
    return nmemb;
}

inline int post(const char* url, const char* body, size_t size, std::string & response) {
    CURL * curl = curl_easy_init();
    curl_easy_setopt(curl, CURLOPT_URL, url);
    curl_easy_setopt(curl, CURLOPT_POST, true);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, body);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, size);
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, onWriteData);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void *) &response);
    curl_easy_setopt(curl, CURLOPT_NOSIGNAL, true);
    curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT_MS, 5000);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, 5000);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, false);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, false);
    curl_easy_setopt(curl, CURLOPT_VERBOSE, false);
    
    int status_code = curl_easy_perform(curl);
    
    curl_easy_cleanup(curl);
    
    return status_code;
}



int main() {
    XMLDocument doc;
    doc.LoadFile("../voc.xml");
    if (doc.ErrorID() != 0) {
        std::cout << "error in parse xml";
        return 0;
    }
    XMLElement* root = doc.RootElement();
    const char* folder = root->FirstChildElement("folder")->GetText();
    const char* filename = root->FirstChildElement("filename")->GetText();
    std::string path = std::string(folder) + "/" + filename;
 
    XMLElement* obj = root->FirstChildElement("object");
    
    json labels = json::array();
    
    while (obj != NULL) {
        json label;
        const char* name = obj->FirstChildElement("name")->GetText();
        XMLElement* bndbox = obj->FirstChildElement("bndbox");
        
        int left = bndbox->FirstChildElement("xmin")->Int64Text();
        int width = bndbox->FirstChildElement("xmax")->Int64Text() - left;
        int top = bndbox->FirstChildElement("ymin")->Int64Text();
        int height = bndbox->FirstChildElement("ymax")->Int64Text() - top;
        
        label["label_name"] = name;
        label["left"] = left;
        label["top"] = top;
        label["width"] = width;
        label["height"] = height;
        labels.push_back(label);
        
        obj = obj->NextSiblingElement();
    }
    std::string image;
    get_file_content(path.c_str(), &image);
    
    std::string base64image = aip::base64_encode(image.c_str(), (int) image.size());
    
    json j;
    j["type"] = "OBJECT_DETECTION";
    j["dataset_id"] = 44820;
    j["entity_name"] = filename;
    j["entity_content"] = base64image;
    j["labels"] = labels;
    
    std::string body = j.dump();
    
    std::string token_path = TOKEN_URL;
    token_path.append("?grant_type=client_credentials");
    token_path.append("&client_id=");
    token_path.append(AK);
    token_path.append("&client_secret=");
    token_path.append(SK);
    
    std::string response;
    
    int status_code = post(token_path.c_str(), NULL, 0, response);
    
    if (status_code != 0) {
        std::cout << "curl error :" << status_code << std::endl;
        return 1;
    }
    
    auto j3 = json::parse(response.c_str());
    
    std::string access_token = j3["access_token"];
    
    std::string api_path = API_URL;
    api_path.append("?access_token=");
    api_path.append(access_token);
    
    std::cout << "access_token=" << access_token << std::endl;
    response.clear();
    status_code = post(api_path.c_str(), body.c_str(), body.size(), response);
    std::cout << response;
    j3 = json::parse(response.c_str());
    
    std::cout << j3;
    return 0;
}

