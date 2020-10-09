package com.example.mechta;


import com.example.mechta.model.Category;
import com.example.mechta.model.MainGroup;
import com.example.mechta.model.Section;
import com.example.mechta.repository.CategoryRepository;
import com.example.mechta.repository.ItemRepository;
import com.example.mechta.repository.MainGroupRepository;
import com.example.mechta.repository.SectionRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SectionParser {
    private static final Logger LOG = LoggerFactory.getLogger(SectionParser.class);

    private static final Set<String> SECTIONS = Set.of("Смартфоны и гаджеты", "Ноутбуки и компьютеры", "Тв, аудио, видео",
            "Техника для дома", "Климат техника", "Кухонная техника", "Встраиваемая техника", "Фото и видео техника", "Активный отдых");

    private static final Set<String> GROUPS_EXCEPTIONS = Set.of("Купить дешевле");
    private static final String URL = "https://www.mechta.kz";

    private static final long ONE_SECOND_MS = 1000L;
    private static final long ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    private static final long ONE_HOUR_MS = 60 * ONE_MINUTE_MS;
    private static final long ONE_DAY_MS = 24 * ONE_HOUR_MS;
    private static final long ONE_WEEK_MS = 7 * ONE_DAY_MS;


    @Value("${sulpak.api.chunk-size}")
    private Integer chunkSize;
    @Value("${sulpak.thread-pool.pool-size}")
    private Integer threadPoolSize;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private MainGroupRepository mainGroupRepository;
    @Autowired
    private ItemRepository itemRepository;


    @Scheduled(fixedDelay = ONE_WEEK_MS)
    @Transactional
    public void getSections() throws IOException {

        Document sectionPage = Jsoup.connect(URL).get();
        LOG.info("Получили главную страницу, ищем секции...");
        Elements sectionElements = sectionPage.select(".aa_hm_items_main");
        for (Element sectionElement : sectionElements) {
            Element sectionElementLink = sectionElement.selectFirst(">a");
            String text = sectionElementLink.text();
            if (SECTIONS.contains(text)) {
                LOG.info("Получаем {}...", text);
                String sectionUrl = sectionElementLink.absUrl("href");
                Section section = sectionRepository.findOneByUrl(sectionUrl)
                        .orElseGet(() -> sectionRepository.save(new Section(text, sectionUrl)));
                LOG.info("Получили {}, ищем группы...", text);

                Elements groupElements = sectionPage.select(".aa_hm_pod2");

                for (Element groupElement : groupElements) {
                    String groupLink = groupElement.selectFirst("a").absUrl("href");
                    String groupText = groupElement.selectFirst("a").text();

                    LOG.info("Получаем {}...", text);
                    MainGroup group = mainGroupRepository.findOneByUrl(sectionUrl)
                            .orElseGet(() -> mainGroupRepository.save(new MainGroup(groupText, groupLink, section)));

                    Elements categoryElements = sectionPage.select(".aa_hm_pod3");

                    for (Element categoryElement : categoryElements) {
                        String categoryLink = categoryElement.selectFirst("a").absUrl("href");
                        String categoryText = categoryElement.selectFirst("a").text();

                        LOG.info("Получаем {}...", text);
                        if (!categoryRepository.existsByUrl(categoryLink)) {
                            categoryRepository.save(new Category(categoryText, categoryLink, group));
                        }

                    }
                }
            }
        }
    }

    @Scheduled(initialDelay = 1200, fixedDelay = ONE_WEEK_MS)
    @Transactional
    public void getAdditionalItemInfo() throws InterruptedException {
        LOG.info("Получаем дополнитульную информацию о товарe...");
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        int page = 0;
        List<Category> categories;
        // 1. offset + limit
        // 2. page + pageSize
        //   offset = page * pageSize;  limit = pageSize;
        while (!(categories = categoryRepository.getChunk(PageRequest.of(page++, chunkSize))).isEmpty()) {
            LOG.info("Получили из базы {} категорий", categories.size());
            CountDownLatch latch = new CountDownLatch(categories.size());
            for (Category category : categories) {
                executorService.execute(new ItemsUpdateTask(itemRepository, category, latch));
            }
            LOG.info("Задачи запущены, ожидаем завершения выполнения...");
            latch.await();
            LOG.info("Задачи выполнены, следующая порция...");
        }
        executorService.shutdown();
    }
}



