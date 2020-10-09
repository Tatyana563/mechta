package com.example.mechta;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test2 {
    private static final Pattern CODE_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(^([0-9]+\\s*)*)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("(background-image: url\\()(.*)(\\))");
    private static final String URL = "https://www.mechta.kz";

    public static void main(String[] args) throws IOException {
        Document itemPage = Jsoup.connect("https://www.mechta.kz/section/apple-eql/").get();

        Elements itemElements = itemPage.select(".aa_section_catalog");
        for (Element itemElement : itemElements) {
            String code = null;
            String price = null;
            String itemUrl = itemElement.selectFirst(".ifont14.j_product_link").absUrl("href");
            String itemText = itemElement.selectFirst(".aa_std_name").text();
            String itemCode = itemElement.selectFirst(".element-table-article.only-desktop").text();
            String itemPrice = itemElement.selectFirst(".aa_std_bigprice.icblack").text();

            Matcher matcher = CODE_PATTERN.matcher(itemCode);
            if (matcher.find()) {
                code = matcher.group(0);
            }
            Matcher priceMatcher = PRICE_PATTERN.matcher(itemPrice);
            if (priceMatcher.find()) {
                price = priceMatcher.group(0).replaceAll("\\s*", "");
            }
           String test= itemElement.selectFirst(".aa_st_imglink.j_product_link").attr("style");
           // String test = "background-image: url('/export/an_files/25f2a37b-e65f-11e9-a22b-005056b6dbd7-104380.jpeg'); height: 100%;";
            Matcher imageMatcher = IMAGE_PATTERN.matcher(test);
            if (imageMatcher.find()) {

                String image = imageMatcher.group(2).replace("'", "");

                String resultURL= URL + image;
                String itemDescription = itemElement.selectFirst(".list-unstyled").text();
            }

        }
    }
}
