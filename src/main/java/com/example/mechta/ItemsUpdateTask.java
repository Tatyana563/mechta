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


    private static final String PAGE_URL_CONSTANT = "?sort=views&page=%d";
    private static final Integer numberOfProductsPerPage = 18;
    private static final Pattern PATTERN = Pattern.compile("Артикул:\\s*(\\S*)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d*\\s*\\d*\\s*\\d*\\s*\\d*\\s*\\d*\\s*\\d*)");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\D*)(\\s*)(\\D*)(\\d*)");

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
        //TODO: complete implementation
        Elements itemElements = firstPage.select(".catalog-container");
        Integer numberofPages = null;
        for (Element itemElement : itemElements) {
            String quantity = itemElement.select(".product-quantity").text();
            Integer amountOfProducts;


            Matcher matcher = QUANTITY_PATTERN.matcher(quantity);
            if (matcher.find()) {
                amountOfProducts = Integer.valueOf(matcher.group(4));

                int main = amountOfProducts / numberOfProductsPerPage;
                if (main != 0) {
                    int tail = amountOfProducts % 18;
                    if ((amountOfProducts % numberOfProductsPerPage != 0)) {
                        numberofPages = main + 1;
                    } else {
                        numberofPages = main;
                    }
                } else {
                    numberofPages = 1;
                }
            }

        }
        return numberofPages;
    }


    private void parseItems(Document itemPage) {

        Elements itemElements = itemPage.select(".aa_vert_cont jq_aa_vert_rec swiper-container swiper-container-horizontal");
        for (Element itemElement : itemElements) {
            Element itemContainer = itemElement.selectFirst(".aa_st_descr iprel");

            String itemUrl = itemElement.selectFirst(".aa_vert_name").attr("href");
            String itemText = itemElement.selectFirst(".aa_vert_name").text();
            String itemPrice = itemElement.selectFirst(".ifont120 icblack").text();


            String price = null;
            Matcher priceMatcher = PRICE_PATTERN.matcher(itemPrice);
            if (priceMatcher.find()) {
                price = priceMatcher.group(1).replaceAll("\\s*", "");
            }
            String itemDescription = itemElement.selectFirst(".list-unstyled").text();
            Matcher matcher = PATTERN.matcher(itemDescription);
            if (matcher.find()) {
                String itemCode = matcher.group(1);

                Item item = itemRepository.findOneByCode(itemCode).orElseGet(() -> new Item(itemCode));

                item.setModel(itemText);
            //    item.setImage(itemPhoto);
                item.setDescription(itemDescription);
                item.setPrice(Double.valueOf(price));
                item.setUrl(itemUrl);
                item.setAvailable(true);
                item.setCategory(category);
                itemRepository.save(item);
            }
        }
    }
}





