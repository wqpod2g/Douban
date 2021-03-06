package nju.iip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Douban {
	
    private static String form_email="";//登录名
	
	private static String form_password="";//密码
	
	private static String redir="";//登录成功后跳转地址
	
	private static CloseableHttpClient httpclient = HttpClients.createDefault();
	
	private static String login_url="http://www.douban.com/accounts/login";//登录页面url
	
	private static String group_url="http://www.douban.com/group/haixiuzu/";//小组地址
	
	
	public static String downloadPic(String PageUrl,String destfilename) throws IOException{
    	Document doc = Jsoup.connect(PageUrl).get();
		Elements p=doc.select("img.captcha_image");
		//System.out.println(p.size());
		String captcha_id="";
		if(p.size()==0){
			 System.out.println("no 验证码~");
		}
		else{
		String url=p.attr("src");
		
		//System.out.println(url);
		
		Pattern p2=Pattern.compile("id=.+&");
		Matcher m2=p2.matcher(url);
		
		while(m2.find()){
			captcha_id=m2.group().substring(3,m2.group().length()-1);
		}
		//System.out.println("captcha_id="+captcha_id);
        // 第一步：先下载验证码到本地
        HttpGet httpget = new HttpGet(url);
        File file = new File(destfilename);
        if (file.exists()) {
            file.delete();
        }
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        try {
            FileOutputStream fout = new FileOutputStream(file);
            byte[] tmp = new byte[2048]; 
            while ((in.read(tmp)) != -1) {
                fout.write(tmp);
            } 
            fout.close();
        } finally {
            in.close();
        }
        httpget.releaseConnection();
		}
        
        return captcha_id;
    }
	
	public static boolean login() throws IOException{
		String captcha_id=downloadPic(login_url,"D:\\yz.png");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("请输入验证码:");
        String yan = br.readLine();
        HttpPost httppost = new HttpPost(login_url);
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("captcha-id", captcha_id));
        params.add(new BasicNameValuePair("captcha-solution", yan));
        params.add(new BasicNameValuePair("form_email", form_email));
        params.add(new BasicNameValuePair("form_password", form_password));
        params.add(new BasicNameValuePair("redir", redir));
        params.add(new BasicNameValuePair("source", "main"));
        params.add(new BasicNameValuePair("login", "登录"));
        httppost.setEntity(new UrlEncodedFormEntity(params));
        CloseableHttpResponse response = httpclient.execute(httppost);
        
        int statuts_code=response.getStatusLine().getStatusCode();
        
        if(statuts_code!=302){
        	System.err.println("登录失败~");
        	return false;
        }
        
        else{
        	System.err.println("登录成功~");
        }
        httppost.releaseConnection();
        return true;
	}
	
	 /**
     * 获取随机评论
     * @return
     * @throws IOException
     */
    public static String getComment() throws IOException{
    	String comment="";
    	List<String>comment_list=new ArrayList<String>();
    	FileInputStream fs=new FileInputStream("D:\\douban\\one.txt");
    	InputStreamReader is=new InputStreamReader(fs,"utf-8");
    	BufferedReader br=new BufferedReader(is);
    	String line=br.readLine();
    	while(line!=null){
    		if(line.length()>1){
    			int index=line.indexOf("。");
    			line=line.substring(0,index+1);
    			comment_list.add(line);
    		}
    		line=br.readLine();
    	}
    	br.close();
    	int num=new Random().nextInt(comment_list.size());//产生0~1的随机数
    	comment=comment_list.get(num);
    	return comment;
    }
    
    
    /**
     * 查找回复小于等于2的帖子
     * @return
     * @throws IOException 
     */
    public static List<String>findTopic() throws IOException{
    	List<String>topic_list=new ArrayList<String>();
    	String pagehtml=getPageHtml(group_url);
    	Document doc=Jsoup.parse(pagehtml);
    	Elements es=doc.select("tr");
    	for(Element e:es){
    		String html=e.html();
    		Pattern pat=Pattern.compile("class=\"\">\\d*</td>");
    		Matcher mat=pat.matcher(html);
    		if(mat.find()){
    			String s=mat.group();
    			if(s.length()==14){
    				Elements p=e.select("a[href]");
            		String url=p.attr("href");
            		topic_list.add(url);
    			}
    		}
    		
    	}
    	return topic_list;
    }
    
    public static String getPageHtml(String url){
    	String html="";
    	try{
    		HttpGet httpget = new HttpGet(url);
        	CloseableHttpResponse response = httpclient.execute(httpget); // 必须是同一个HttpClient！
        	HttpEntity entity = response.getEntity();
        	html = EntityUtils.toString(entity, "GBK");
        	httpget.releaseConnection();
        	return html;
    	}catch(Exception e){
    		//e.printStackTrace();
    		return html;
    	}
    }
    
    public static boolean startPost(String url) {
    	
    	try{
    		String html=getPageHtml(url);
       		Pattern p=Pattern.compile("呃...你想要的东西不在这儿");
       		Matcher m=p.matcher(html);
       		if(m.find()){
       			return false;
       		}
       		
       		Pattern p3=Pattern.compile("该话题已被小组管理员设为不允许回应");
       		Matcher m3=p3.matcher(html);
       		if(m3.find()){
       			return false;
       		}
       		
       		Pattern p2=Pattern.compile("请输入上图中的单词");
       		Matcher m2=p2.matcher(html);
       		if(m2.find()){
       			System.out.println("要输验证码了~暂停10分钟");
       		    Thread.sleep(600000);
       		    return false;
       		}
       		 
       	    HttpPost httppost = new HttpPost(url+"add_comment#last");
       	    httppost.addHeader("Connection", "keep-alive");
            List<NameValuePair> params2 = new ArrayList<NameValuePair>();
            params2.add(new BasicNameValuePair("ck", "xNxg"));
            params2.add(new BasicNameValuePair("rv_comment",getComment()));
            params2.add(new BasicNameValuePair("start", "0"));
            params2.add(new BasicNameValuePair("submit_btn", "加上去"));
            httppost.setEntity(new UrlEncodedFormEntity(params2,"utf-8"));
            CloseableHttpResponse response = httpclient.execute(httppost);
            int status_code=response.getStatusLine().getStatusCode();
            
            if(status_code==302){
           	 System.out.println("评论成功~ "+url);//评论成功
            }
            else{
            	 System.out.println("评论失败~ "+url);//评论失败
            }
            httppost.releaseConnection();
            Thread.sleep(1500);
            
    	}catch(Exception e){
   		 return false;
   	 }
   	 
   	 return true;
   }
	
	public static void main(String[] args) throws Exception{
		login();
		while(true){
    		List<String>topic_list=findTopic();
    		for(String url:topic_list){
    			startPost(url);
    		}
			Thread.sleep(10000); // 设置暂停毫秒，防止引起豆瓣注意， 这个时间可长可短，根据需要
			System.out.println("----------------------");  
    	}
	}

}
