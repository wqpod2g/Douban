package nju.iip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
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
	
    private static String form_email="903183867@qq.com";//登录名
	
	private static String form_password="wqchina007008";//密码
	
	private static String redir="http://www.douban.com/people/mrnevermore/";//登录成功后跳转地址
	
	private static CloseableHttpClient httpclient = HttpClients.createDefault();
	
	private static String login_url="http://www.douban.com/accounts/login";//登录页面url
	
	private static String group_url="http://www.douban.com/group/kaopulove/";//小组地址
	
    private static int retry_times=0;
	
	private static int count=0;
	
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
            int a = -1;
            byte[] tmp = new byte[2048]; 
            while ((a = in.read(tmp)) != -1) {
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
        
       // System.out.println("response.getStatusLine()="+statuts_code);// 返回302
        if(statuts_code!=302){
        	System.err.println("登录失败~");
        	return false;
        }
        
        else{
        	System.err.println("登录成功~");
        }
        
        Header locationHeader = response.getFirstHeader("Location");
    //    System.out.println("locationHeader.getValue()="+locationHeader.getValue());
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
//    			else{
//    			String num=s.substring(9,mat.group().length()-5);
//    			int reply_num=Integer.parseInt(num);
//    			Elements p=e.select("a[href]");
//        		String url=p.attr("href");  
//        		if(reply_num<=1){
//        			topic_list.add(url);
//        			//System.out.println(url);
//        		}
//        		
//    			}
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
   	    // System.out.println("html="+html);
   		Pattern p=Pattern.compile("呃...你想要的东西不在这儿");
   		Matcher m=p.matcher(html);
   		if(m.find()){
   			return false;
   		}
   		 
   		
   	    HttpPost httppost = new HttpPost(url+"add_comment#last");
        List<NameValuePair> params2 = new ArrayList<NameValuePair>();
        params2.add(new BasicNameValuePair("ck", "GWc8"));
        params2.add(new BasicNameValuePair("rv_comment",getComment()));
        params2.add(new BasicNameValuePair("start", "0"));
        params2.add(new BasicNameValuePair("submit_btn", "加上去"));
        httppost.setEntity(new UrlEncodedFormEntity(params2,"utf-8"));
        CloseableHttpResponse response = httpclient.execute(httppost);
        int status_code=response.getStatusLine().getStatusCode();
        
        if(status_code==302){
       	 System.out.println("评论成功~ "+url);//评论成功
       	 retry_times=0;  
        }
        else{
       	 System.out.println("评论失败~ "+url);//评论失败
       	 long time= System.currentTimeMillis();
       	 Date date=new Date(time);
       	 System.out.println(date.toLocaleString());  
       	 retry_times++;
//       	 if(retry_times>=3){
//       		 System.out.println("暂停一小时~");
//       		 Thread.sleep(3600000);
//       	 }
       	 
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
    			//System.out.println(url);
    		}
			Thread.sleep(10000); // 设置暂停毫秒，防止引起豆瓣注意， 这个时间可长可短，根据需要
			System.out.println("----------------------");  
    	}
	}

}
