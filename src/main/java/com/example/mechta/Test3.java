package com.example.mechta;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Test3 {
    public static void main(String[] args) throws IOException {
        Document itemPage = Jsoup.connect("https://www.mechta.kz/section/naushniki/").get();

        Element itemElements = itemPage.selectFirst(".modern-page-navigation>a:nth-last-of-type(2)");

        String text = itemElements.text();
    }
}
