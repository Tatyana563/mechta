package com.example.mechta;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class Test2 {
    public static void main(String[] args) throws IOException {
        Document itemPage = Jsoup.connect("https://www.mechta.kz/section/apple-eql/").get();

        Elements itemElements = itemPage.select(".aa_section_catalog");
        for (Element itemElement : itemElements) {
         //   Element itemContainer = itemElement.selectFirst(".inner");

            String itemUrl = itemElement.selectFirst(".ifont14.j_product_link").absUrl("href");
            String itemText = itemElement.selectFirst(".aa_std_name").text();


        }
    }
}
