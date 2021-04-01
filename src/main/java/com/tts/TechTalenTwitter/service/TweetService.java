package com.tts.TechTalenTwitter.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tts.TechTalenTwitter.model.Tag;
import com.tts.TechTalenTwitter.model.Tweet;
import com.tts.TechTalenTwitter.model.TweetDisplay;
import com.tts.TechTalenTwitter.model.User;
import com.tts.TechTalenTwitter.repository.TagRepository;
import com.tts.TechTalenTwitter.repository.TweetRepository;

@Service
public class TweetService {

    @Autowired
    private TweetRepository tweetRepository;
    
    @Autowired
    private TagRepository tagRepository;
    

    public List<TweetDisplay> findAll() { //changed from List<Tweet> to List<TweetDisplay>
        List<Tweet> tweets = tweetRepository.findAllByOrderByCreatedAtDesc();
        return formatTweets(tweets);  //changed from tweets to formatTweets(tweets)
    }
    
    public List<TweetDisplay> findAllByUser(User user) {
        List<Tweet> tweets = tweetRepository.findAllByUserOrderByCreatedAtDesc(user);
        return formatTweets(tweets);
    }
    
    public List<TweetDisplay> findAllByUsers(List<User> users){
        List<Tweet> tweets = tweetRepository.findAllByUserInOrderByCreatedAtDesc(users);
        return formatTweets(tweets);
    }
    
    public List<TweetDisplay> findAllWithTag(String tag) { //have to add methods to tweet repository for function to work
        List<Tweet> tweets = tweetRepository.findByTags_PhraseOrderByCreatedAtDesc(tag);
        return formatTweets(tweets);
    }
    
    public void save(Tweet tweet) {
        handleTags(tweet);
        tweetRepository.save(tweet);
    }
    
    //creates a regular expression pattern that searches for # and then some non white space "#\\"
    private void handleTags(Tweet tweet) {
        List<Tag> tags = new ArrayList<Tag>();
        Pattern pattern = Pattern.compile("#\\w+");
        Matcher matcher = pattern.matcher(tweet.getMessage());
        //can walk and return matches
        while (matcher.find()) {  
            String phrase = matcher.group().substring(1).toLowerCase();
            Tag tag = tagRepository.findByPhrase(phrase); //see if hashtag exists if so, grab information about it
            if (tag == null) {     //if it doesn't we make a new one
                tag = new Tag();
                tag.setPhrase(phrase);  //set phrase to what hashtag is and then save it
                tagRepository.save(tag);
            }
            tags.add(tag); //add to list in repository
        }
        tweet.setTags(tags);
    }

    private List<TweetDisplay> formatTweets(List<Tweet> tweets) { //changed from <Tweet> to <TweetDisplay>
        addTagLinks(tweets);
        shortenLinks(tweets); //shorten url links. have to create the method to make it work
        List<TweetDisplay> displayTweets = formatTimestamps(tweets);
        return displayTweets;
    }

    private void addTagLinks(List<Tweet> tweets) {
        Pattern pattern = Pattern.compile("#\\w+");
        for (Tweet tweet : tweets) {
            String message = tweet.getMessage();
            Matcher matcher = pattern.matcher(message);
            Set<String> tags = new HashSet<String>();  //set is same as list with no order
            while (matcher.find()) {
                tags.add(matcher.group());
            }
            for (String tag : tags) {
                String replacement =  "<a class=\"tag\" href=\"/tweets/";
                replacement += tag.substring(1).toLowerCase();
                replacement += "\">";
                replacement += tag;
                replacement += "</a>";
                message = message.replaceAll(tag, replacement); //replace tag with hyperlink, so tags are linked
            }
            tweet.setMessage(message);
        }
    }

    private void shortenLinks(List<Tweet> tweets) {
        Pattern pattern = Pattern.compile("https?[^ ]+");
        for (Tweet tweet : tweets) {
            String message = tweet.getMessage();
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String link = matcher.group();
                String shortenedLink = link;
                if (link.length() > 23) {
                    shortenedLink = link.substring(0, 20) + "...";
                    String replacementLink = "<a class=\"tag\" href=\"";
                    replacementLink += link;
                    replacementLink += "\" target=\"_blank\">";
                    replacementLink += shortenedLink;
                    replacementLink += "</a>";
                    message = message.replace(link, replacementLink);   
              }
                tweet.setMessage(message);
            }

        }
    }
        
        private List<TweetDisplay> formatTimestamps(List<Tweet> tweets) 
        {
            List<TweetDisplay> response = new ArrayList<>();
            PrettyTime prettyTime = new PrettyTime();
            SimpleDateFormat simpleDate = new SimpleDateFormat("M/d/yy");
            Date now = new Date();
            for (Tweet tweet : tweets) {
                TweetDisplay tweetDisplay = new TweetDisplay();
                tweetDisplay.setUser(tweet.getUser());
                tweetDisplay.setMessage(tweet.getMessage());
                tweetDisplay.setTags(tweet.getTags());
                long diffInMillies = Math.abs(now.getTime() - tweet.getCreatedAt().getTime());
                long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                if (diff > 3) {
                    tweetDisplay.setDate(simpleDate.format(tweet.getCreatedAt()));
                } else {
                    tweetDisplay.setDate(prettyTime.format(tweet.getCreatedAt()));
                }
                response.add(tweetDisplay);
            }
            return response;
        }
    
    
    
}