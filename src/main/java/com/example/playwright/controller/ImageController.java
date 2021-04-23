package com.example.playwright.controller;


import com.example.playwright.domain.ImageResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.ipdata.client.Ipdata;
import io.ipdata.client.error.IpdataException;
import io.ipdata.client.model.IpdataModel;
import io.ipdata.client.service.IpdataService;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HttpsURLConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.cert.Certificate;

@RestController
public class ImageController {

    @RequestMapping(value = "/sendimage", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ImageResponse sendImage(@RequestParam String url) throws IOException, IpdataException, JSONException {
        URL destinationURL=new URL(url);
        ImageResponse imageResponse=new ImageResponse();
        Playwright playwright=Playwright.create();
        Browser browser=playwright.webkit().launch();
        BrowserContext context = browser.newContext();
        Page page = context.newPage();
        page.navigate(url);
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot" + ".jpg")));
        String content= page.content();
        //System.out.println(content);

        String ipAddress = getIPAddress(url, imageResponse);
        generateAsn(imageResponse, ipAddress);
        generateCertificate(url, destinationURL, imageResponse);
        generateImages(url, imageResponse);
        return imageResponse;


    }
    /**To get Additional certificatesn
     * for (Certificate cert : certs) {
     System.out.println("Certificate is: " + cert);
     if (cert instanceof X509Certificate) {
     X509Certificate x = (X509Certificate) cert;
     System.out.println(x.getIssuerDN());
     }
     **/
    private void generateCertificate(String url, URL destinationURL, ImageResponse imageResponse) throws IOException {
        if(url.contains("https")) {
            HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            imageResponse.setCertificate(certs[0]);
        }
    }

    private void generateAsn(ImageResponse imageResponse, String ipAddress) throws MalformedURLException, IpdataException {
        URL u = new URL("https://api.ipdata.co");
        IpdataService ipdataService = Ipdata.builder().url(u)
                .key("7f6d93447341e3f9d1dfe241627eae6476a9394b1c040717dafc83c1").get();
        IpdataModel model = ipdataService.ipdata(ipAddress);
        String asn= model.asn().asn();
        imageResponse.setAsn(asn);
    }

    /**String keywords = doc.select("meta[name=keywords]").first().attr("content");
        System.out.println("Meta keyword : " + keywords);
        *String description = doc.select("meta[name=description]").get(0).attr("content");
        System.out.println("Meta description : " + description);**/
    private void generateImages(String url, ImageResponse imageResponse) throws IOException {
        Document doc = Jsoup.connect(url).get();
        imageResponse.setPageTitle(doc.title());
        Elements images = doc.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
        for (Element image : images) {
            System.out.println("src : " + image.attr("src"));
            System.out.println("height : " + image.attr("height"));
            System.out.println("width : " + image.attr("width"));
            System.out.println("alt : " + image.attr("alt"));
        }

    }

    private String getIPAddress(String url, ImageResponse imageResponse) throws UnknownHostException, MalformedURLException {
        InetAddress address = InetAddress.getByName(new URL(url).getHost());
        String ipAddress=address.getHostAddress();
        imageResponse.setIpAddress(ipAddress);
        return ipAddress;
    }

    @RequestMapping(value = "/sid", method = RequestMethod.GET,
            produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<InputStreamResource> getImage() throws IOException {

        var imgFile = new ClassPathResource("screenshot.jpg");

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(new InputStreamResource(imgFile.getInputStream()));
    }



}
