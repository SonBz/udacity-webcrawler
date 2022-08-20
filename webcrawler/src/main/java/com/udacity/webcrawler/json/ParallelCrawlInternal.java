package com.udacity.webcrawler.json;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

public class ParallelCrawlInternal extends RecursiveTask<Boolean> {
    private String url;
    private Instant deadlines;
    private int maxDepth;
    private Clock clock;
    private PageParserFactory parserFactory;
    private ConcurrentMap<String, Integer> counts;
    private ConcurrentSkipListSet<String> visitedUrls;
    private List<Pattern> ignoredUrls;

    public ParallelCrawlInternal(String url, Instant deadlines, int maxDepth,
                                 Clock clock, PageParserFactory parserFactory,
                                 ConcurrentMap<String, Integer> counts,
                                 ConcurrentSkipListSet<String> visitedUrls, List<Pattern> ignoredUrls) {
        this.url = url;
        this.deadlines = deadlines;
        this.maxDepth = maxDepth;
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected Boolean compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadlines)) {
            return false;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return false;
            }
        }
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
            if (visitedUrls.contains(url)) {
                return false;
            }
            visitedUrls.add(url);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            lock.unlock();
        }

        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }
        }

        List<ParallelCrawlInternal> subtasks= new ArrayList<>();
        for (String link : result.getLinks()) {
            subtasks.add(new ParallelCrawlInternal(link, deadlines, maxDepth - 1, clock, parserFactory, counts, visitedUrls,ignoredUrls));
        }

        invokeAll(subtasks);
        return true;
    }
}
