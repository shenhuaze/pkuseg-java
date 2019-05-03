package com.huaze.shen.feature;

import java.io.*;
import java.util.*;

/**
 * @author Huaze Shen
 * @date 2019-04-19
 *
 * 提取特征
 */
public class FeatureExtractor {
    private String trainFile;
    private Set<String> unigramSet;
    private Set<String> bigramSet;
    private List<List<String>> charLists;
    private Map<String, Integer> featureIndexMap;
    private Map<String, Integer> tagIndexMap;
    private String featureSaveDir;
    private int wordMax = 6;
    private int wordMin = 2;

    public FeatureExtractor(String trainFile) {
        this.trainFile = trainFile;
        init();
    }

    private void init() {
        unigramSet = new HashSet<>();
        bigramSet = new HashSet<>();
        charLists = new ArrayList<>();
        featureIndexMap = new HashMap<>();
        tagIndexMap = new HashMap<>();
        featureSaveDir = "src/main/resources/data/feature_save/";
        buildFeature();
        saveFeature();
    }

    private void buildFeature() {
        try {
            InputStream inputStream = FeatureExtractor.class.getClassLoader().getResourceAsStream(trainFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                String specialCharNormalizedLine = specialCharNormalize(line);
                String[] lineSplit = specialCharNormalizedLine.split("[ \t]+");
                unigramSet.addAll(Arrays.asList(lineSplit));
                for (int i = 0; i < lineSplit.length - 1; i++) {
                    bigramSet.add(String.format("%s*%s", lineSplit[i], lineSplit[i + 1]));
                }
                List<String> charList = new ArrayList<>();
                for (String word : lineSplit) {
                    for (int i = 0; i < word.length(); i++) {
                        char ch = word.charAt(i);
                        String numberLetterNormalizedChar = numberLetterNormalize(ch);
                        charList.add(numberLetterNormalizedChar);
                    }
                }
                charLists.add(charList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO: 去除出现频次小于阈值的特征
        createFeatures();
    }

    private void saveFeature() {
        saveUnigram();
        saveBigram();
        saveFeatureIndexMap();
        saveTagIndexMap();
    }

    private void createFeatures() {
        Set<String> featureSet = new HashSet<>();
        for (List<String> charList : charLists) {
            for (int i = 0; i < charList.size(); i++) {
                 List<String> nodeFeatures = getNodeFeatures(i, charList);
                 featureSet.addAll(nodeFeatures);
            }
        }
        featureToIndex(featureSet);
        tagToIndex();
    }

    private List<String> getNodeFeatures(int index, List<String> charList) {
        List<String> nodeFeatures = new ArrayList<>();
        createCharacterBasedFeature(nodeFeatures, index, charList);
        createWordBasedFeature(nodeFeatures, index, charList);
        return nodeFeatures;
    }

    private void createCharacterBasedFeature(List<String> nodeFeatures, int index, List<String> charList) {
        // 1 start feature
        nodeFeatures.add("$$");

        // 8 unigramSet/bigramSet feature
        // center
        nodeFeatures.add("c." + charList.get(index));
        // left1
        if (index > 0) {
            nodeFeatures.add("c-1." + charList.get(index - 1));
        } else {
            nodeFeatures.add("/");
        }
        // right1
        if (index < charList.size() - 1) {
            nodeFeatures.add("c1." + charList.get(index + 1));
        } else {
            nodeFeatures.add("/");
        }
        // left2
        if (index > 1) {
            nodeFeatures.add("c-2." + charList.get(index - 2));
        } else {
            nodeFeatures.add("/");
        }
        // right2
        if (index < charList.size() - 2) {
            nodeFeatures.add("c2." + charList.get(index + 2));
        } else {
            nodeFeatures.add("/");
        }
        // left1 + center
        if (index > 0) {
            nodeFeatures.add("c-1c." + charList.get(index - 1) + "." + charList.get(index));
        } else {
            nodeFeatures.add("/");
        }
        // center + right1
        if (index < charList.size() - 1) {
            nodeFeatures.add("cc1." + charList.get(index) + "." + charList.get(index + 1));
        } else {
            nodeFeatures.add("/");
        }
        // left2 + left1
        if (index > 1) {
            nodeFeatures.add("c-2c-1." + charList.get(index - 2) + "." + charList.get(index - 1));
        } else {
            nodeFeatures.add("/");
        }
        // right1 + right2
        if (index < charList.size() - 2) {
            nodeFeatures.add("c1c2." + charList.get(index + 1) + "." + charList.get(index + 2));
        } else {
            nodeFeatures.add("/");
        }
    }

    private void createWordBasedFeature(List<String> nodeFeatures, int index, List<String> charList) {
        List<String> prevInList = createPrevInList(nodeFeatures, index, charList);
        List<String> postInList = createPostInList(nodeFeatures, index, charList);
        List<String> prevExcludeList = createPrevExcludeList(index, charList);
        List<String> postExcludeList = createPostExcludeList(index, charList);
        createLeftBigramWordFeature(prevExcludeList, postInList, nodeFeatures);
        createRightBigramWordFeature(prevInList, postExcludeList, nodeFeatures);
    }

    private List<String> createPrevInList(List<String> nodeFeatures, int index, List<String> charList) {
        List<String> prevInList = new ArrayList<>();
        for (int range = wordMin; range <= wordMax; range++) {
            String tempWord = getSubstring(index - range + 1, range, charList);
            if ("".equals(tempWord)) {
                nodeFeatures.add("/");
                prevInList.add("**noWord");
            } else {
                if (unigramSet.contains(tempWord)) {
                    nodeFeatures.add("w-1." + tempWord);
                    prevInList.add(tempWord);
                } else {
                    nodeFeatures.add("/");
                    prevInList.add("**noWord");
                }
            }
        }
        return prevInList;
    }

    private List<String> createPostInList(List<String> nodeFeatures, int index, List<String> charList) {
        List<String> postInList = new ArrayList<>();
        for (int range = wordMin; range <= wordMax; range++) {
            String tempWord = getSubstring(index, range, charList);
            if ("".equals(tempWord)) {
                nodeFeatures.add("/");
                postInList.add("**noWord");
            } else {
                if (unigramSet.contains(tempWord)) {
                    nodeFeatures.add("w1." + tempWord);
                    postInList.add(tempWord);
                } else {
                    nodeFeatures.add("/");
                    postInList.add("**noWord");
                }
            }
        }
        return postInList;
    }
    
    private List<String> createPrevExcludeList(int index, List<String> charList) {
        List<String> prevExcludeList = new ArrayList<>();
        for (int range = wordMin; range <= wordMax; range++) {
            String tempWord = getSubstring(index - range, range, charList);
            if ("".equals(tempWord)) {
                prevExcludeList.add("**noWord");
            } else {
                if (unigramSet.contains(tempWord)) {
                    prevExcludeList.add(tempWord);
                } else {
                    prevExcludeList.add("**noWord");
                }
            }
        }
        return prevExcludeList;
    }

    private List<String> createPostExcludeList(int index, List<String> charList) {
        List<String> postExcludeList = new ArrayList<>();
        for (int range = wordMin; range <= wordMax; range++) {
            String tempWord = getSubstring(index + 1, range, charList);
            if (".".equals(tempWord)) {
                postExcludeList.add("**noWord");
            } else {
                if (unigramSet.contains(tempWord)) {
                    postExcludeList.add(tempWord);
                } else {
                    postExcludeList.add("**noWord");
                }
            }
        }
        return postExcludeList;
    }

    private void createLeftBigramWordFeature(List<String> prevExcludeList,
                                             List<String> postInList,
                                             List<String> nodeFeatures) {
        for (String prevExcludeWord : prevExcludeList) {
            for (String postInWord : postInList) {
                String bigram = prevExcludeWord + "*" + postInWord;
                if (bigramSet.contains(bigram)) {
                    nodeFeatures.add("ww.l." + bigram);
                } else {
                    nodeFeatures.add("/");
                }
            }
        }
    }

    private void createRightBigramWordFeature(List<String> prevInList,
                                              List<String> postExcludeList,
                                              List<String> nodeFeatures) {
        for (String prevInWord : prevInList) {
            for (String postExcludeWord : postExcludeList) {
                String bigram = prevInWord + "*" + postExcludeWord;
                if (bigramSet.contains(bigram)) {
                    nodeFeatures.add("ww.r." + bigram);
                } else {
                    nodeFeatures.add("/");
                }
            }
        }
    }

    private String getSubstring(int startIndex, int range, List<String> charList) {
        if (startIndex < 0) {
            return "";
        }
        if (startIndex >= charList.size()) {
            return "";
        }
        if (startIndex + range >= charList.size() + 1) {
            return "";
        }
        return String.join("", charList.subList(startIndex, startIndex + range));
    }

    private void featureToIndex(Set<String> featureSet) {
        for (String feature : featureSet) {
            if (!featureIndexMap.containsKey(feature)) {
                featureIndexMap.put(feature, featureIndexMap.size());
            }
        }
    }

    private void tagToIndex() {
        String[] tags = {"B", "M", "E", "S"};
        for (int i = 0; i < tags.length; i++) {
            tagIndexMap.put(tags[i], i);
        }
    }

    private void saveUnigram() {
        String unigramFile = featureSaveDir + "unigram.txt";
        writeSetToFile(unigramSet, unigramFile);
    }

    private void saveBigram() {
        String bigramFile = featureSaveDir + "bigram.txt";
        writeSetToFile(bigramSet, bigramFile);
    }

    private void saveFeatureIndexMap() {
        String featureIndexFile = featureSaveDir + "feature_index.txt";
        writeMapToFile(featureIndexMap, featureIndexFile);
    }

    private void saveTagIndexMap() {
        String tagIndexFile = featureSaveDir + "tag_index.txt";
        writeMapToFile(tagIndexMap, tagIndexFile);
    }

    private void writeSetToFile(Set<String> set, String file) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            for (String word : set) {
                bufferedWriter.write(word + "\n");
            }
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeMapToFile(Map<String, Integer> map, String file) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                String feature = entry.getKey();
                Integer index = entry.getValue();
                bufferedWriter.write(feature + "\t" + index + "\n");
            }
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String specialCharNormalize(String line) {
        StringBuilder stringBuilder = new StringBuilder();
        char[] specialChars = "-._,|/*:".toCharArray();
        Set<Character> specialCharSet = new HashSet<>();
        for (char ch : specialChars) {
            specialCharSet.add(ch);
        }
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (specialCharSet.contains(ch)) {
                stringBuilder.append('&');
            } else {
                stringBuilder.append(ch);
            }
        }
        return stringBuilder.toString();
    }

    private String numberLetterNormalize(char ch) {
        char[] numberChars = "0123456789.几二三四五六七八九十千万亿兆零１２３４５６７８９０％".toCharArray();
        char[] letterChars = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｇｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ／・－".toCharArray();
        Set<Character> numberCharSet = new HashSet<>();
        for (char c : numberChars) {
            numberCharSet.add(c);
        }
        Set<Character> letterCharSet = new HashSet<>();
        for (char c : letterChars) {
            letterCharSet.add(c);
        }
        if (numberCharSet.contains(ch)) {
            return "**Num";
        }
        if (letterCharSet.contains(ch)) {
            return "**Letter";
        }
        return String.valueOf(ch);
    }

    public static void main(String[] args) {
        String trainFile = "data/pku_test_gold.utf8";
        FeatureExtractor featureExtractor = new FeatureExtractor(trainFile);
    }
}
