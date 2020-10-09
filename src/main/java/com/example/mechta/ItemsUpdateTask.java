package com.example.mechta;


import com.example.mechta.model.Category;
import com.example.mechta.model.Item;
import com.example.mechta.repository.ItemRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ItemsUpdateTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ItemsUpdateTask.class);

    private final ItemRepository itemRepository;
    private final Category category;
    private final CountDownLatch latch;

    private static final String URL = "https://www.mechta.kz";
    private static final String PAGE_URL_CONSTANT = "?PAGEN_2=%d&sort=popular&adesc=asc";
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d*\\s*\\d*\\s*\\d*\\s*\\d*\\s*\\d*\\s*\\d*)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("(background-image: url\\()(.*)(\\))");
    private static final Pattern CODE_PATTERN = Pattern.compile("(\\d+)");

    public ItemsUpdateTask(ItemRepository itemRepository, Category category, CountDownLatch latch) {
        this.itemRepository = itemRepository;
        this.category = category;
        this.latch = latch;
    }


    @Override
    public void run() {
        try {
            itemRepository.resetItemAvailability(category);
            String categoryUrl = category.getUrl();
            String firstPageUrl = String.format(categoryUrl + PAGE_URL_CONSTANT, 1);

            Document firstPage = Jsoup.connect(firstPageUrl).get();

            int totalPages = getTotalPages(firstPage);
            parseItems(firstPage);
            for (int i = 2; i <= totalPages; i++) {
                LOG.info("Получаем список товаров - страница {}", i);
                parseItems(Jsoup.connect(String.format(categoryUrl + PAGE_URL_CONSTANT, i)).get());

            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            latch.countDown();
        }
    }

    private int getTotalPages(Document firstPage) {
        Element lastPage = firstPage.selectFirst(".modern-page-navigation>a:nth-last-of-type(2)");
        if (lastPage != null) {
            String text = lastPage.text();
            return Integer.parseInt(text);
        }
        return 0;
    }


    private void parseItems(Document itemPage) {
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
            String test = itemElement.selectFirst(".aa_st_imglink.j_product_link").attr("style");

            Matcher imageMatcher = IMAGE_PATTERN.matcher(test);
            if (imageMatcher.find()) {

                String image = imageMatcher.group(2).replace("'", "");

                String imageURL = URL + image;

                Item item = itemRepository.findOneByCode(itemCode).orElseGet(() -> new Item(itemCode));

                item.setModel(itemText);
                item.setImage(imageURL);
                item.setPrice(Double.valueOf(price));
                item.setUrl(itemUrl);
                item.setAvailable(true);
                item.setCategory(category);
                itemRepository.save(item);
            }
        }
    }
}






