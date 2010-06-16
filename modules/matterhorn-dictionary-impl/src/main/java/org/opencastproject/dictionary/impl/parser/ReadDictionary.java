/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.dictionary.impl.parser;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ReadDictionary {
  private static HashMap<Long, Integer> dict;
  private static String[] wordList;
  private static final int BUFFER_LENGTH = 10 * 1024 * 1024;

  static Console c = System.console();

  public static void main(String args[]) throws Exception {
    String lang = "en";
    String root = "/opt/matterhorn/bp/ParseWiki/results/" + lang + "/";
    String dictPath = root + lang + ".dict";
    String wlPath = root + lang + ".wordlist.csv";
    System.out.println("-----------------------");
    System.out.println("Loading " + lang + " dictionary");
    dict = loadDictionary(dictPath);
    //wordList = loadWordList(wlPath);
    System.out.println("Dictionary loaded!");
    System.out.println("-----------------------");
    
    Integer allW = 0;
    for(Entry<Long, Integer> e : dict.entrySet()){
      allW+=e.getValue();
    }
    System.out.println(allW);
//    // csv
//    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
//        new FileOutputStream(lang + ".csv"), "UTF8"));
//    int i = 0;
//    for (String w : wordList) {
//      if (++i % 100000 == 0)
//        System.out.println(i);
//      Integer count = getCount(w);
//      if (count > 0) {
//        bw.write(w);
//        bw.write(',');
//        bw.write(count.toString());
//        bw.newLine();
//      } else{
//        int a = 23;
//        a++;
//      }
//    }
//    bw.close();


//    // check words
//    while (true) {
//      c.printf("Check word: ");
//      String w = c.readLine().toUpperCase();
//      long hash = StringUtil.hash(w);
//      if (dict.containsKey(hash))
//        c.printf("%s found %d times\n", w, dict.get(hash));
//      else
//        c.printf("%s was not found!\n", w);
//      if (w.equals("EXIT")) {
//        c.printf("Exiting program!\n\n");
//        break;
//      }
//    }

    // // get stopwords
    // long allCount = 0l;
    // TreeMap<String, Integer> stopWords = new TreeMap<String, Integer>();
    //
    // for (String w : wordList) {
    // Integer count = getCount(w);
    // allCount += count;
    // if (count > 20000) {
    // // System.out.println(w+": "+count);
    // stopWords.put(w, count);
    // }
    // }
    // Map<String, Integer> res = sortByValue(stopWords);
    // for (Entry<String, Integer> sw : res.entrySet()) {
    // System.out.println(sw.getKey()+": "+sw.getValue());
    // }
    // System.out.println("-------------------------------");
    // System.out.println("allCount: " + allCount);
  }

  public static Integer getCount(String word) {
    word = word.toUpperCase();
    Long hash = StringUtil.hash(word);
    return getCount(hash);
  }

  public static Integer getCount(Long hash) {
    try {
      int count = dict.get(hash);
      // System.out.println(count);
      return count;
    } catch (Exception e) {
      // System.out.println("Not found");
    }
    return 0;
  }

  private static HashMap<Long, Integer> loadDictionary(String uri) {
    // dict object
    HashMap<Long, Integer> result = new HashMap<Long, Integer>();
    FileInputStream fis;
    try {
      fis = new FileInputStream(uri);
      ObjectInputStream ois = new ObjectInputStream(fis);
      try { // catch eof
        while (true) {
          Long hash = ois.readLong();
          Integer count = ois.readInt();
          result.put(hash, count);
        }
      } catch (Exception e) {
        // eof
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private static String[] loadWordList(String uri) {
    FileInputStream fis;
    String csv = "";
    try {
      fis = new FileInputStream(uri);
      BufferedReader br = new BufferedReader(new InputStreamReader(fis), BUFFER_LENGTH);
      csv = br.readLine();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return csv.split(",");
  }

  @SuppressWarnings("unchecked")
  static Map sortByValue(Map map) {
    List list = new LinkedList(map.entrySet());
    Collections.sort(list, new Comparator() {
      public int compare(Object o1, Object o2) {
        return -((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
      }
    });
    Map result = new LinkedHashMap();
    for (Iterator it = list.iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      result.put(entry.getKey(), entry.getValue());
    }
    return result;
  }

}
