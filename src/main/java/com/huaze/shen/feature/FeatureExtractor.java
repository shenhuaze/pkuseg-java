package com.huaze.shen.feature;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author Huaze Shen
 * @date 2019-04-19
 *
 * 提取特征
 */
public class FeatureExtractor {
    private String trainFile;
    private Set<String> unigram;
    private Set<String> bigram;
    private List<List<String>> examples;
    private Set<String> featureSet;

    public FeatureExtractor(String trainFile) {
        this.trainFile = trainFile;
        unigram = new HashSet<>();
        bigram = new HashSet<>();
        examples = new ArrayList<>();
        featureSet = new HashSet<>();
    }

    public void build() {
        try {
            InputStream inputStream = FeatureExtractor.class.getClassLoader().getResourceAsStream(trainFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                // TODO: 特殊符号替换为'&'
                String[] lineSplit = line.split("[ \t]+");
                unigram.addAll(Arrays.asList(lineSplit));
                for (int i = 0; i < lineSplit.length - 1; i++) {
                    bigram.add(String.format("%s*%s", lineSplit[i], lineSplit[i + 1]));
                }
                List<String> example = new ArrayList<>();
                for (String word : lineSplit) {
                    for (int i = 0; i < word.length(); i++) {
                        // TODO: 字母、数字归一化
                        example.add(String.valueOf(word.charAt(i)));
                    }
                }
                examples.add(example);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // TODO: 去除出现频次小于阈值的特征
        createFeatures();
    }

    private void createFeatures() {
        for (List<String> example : examples) {
            for (int i = 0; i < example.size(); i++) {
                 List<String> nodeFeatures = getNodeFeatures(i, example);
                 featureSet.addAll(nodeFeatures);
            }
        }
    }

    private List<String> getNodeFeatures(int index, List<String> example) {
        List<String> nodeFeatures = new ArrayList<>();
        createCharacterBasedFeature(nodeFeatures, index, example);
        createWordBasedFeature(nodeFeatures, index, example);
        return nodeFeatures;
    }

    private void createCharacterBasedFeature(List<String> nodeFeatures, int index, List<String> example) {
        // 1 start feature
        nodeFeatures.add("$$");

        // 8 unigram/bigram feature
        // center
        nodeFeatures.add("c." + example.get(index));
        // left1
        if (index > 0) {
            nodeFeatures.add("c-1." + example.get(index - 1));
        } else {
            nodeFeatures.add("/");
        }
        // right1
        if (index < example.size() - 1) {
            nodeFeatures.add("c1." + example.get(index + 1));
        } else {
            nodeFeatures.add("/");
        }
        // left2
        if (index > 1) {
            nodeFeatures.add("c-2." + example.get(index - 2));
        } else {
            nodeFeatures.add("/");
        }
        // right2
        if (index < example.size() - 2) {
            nodeFeatures.add("c2." + example.get(index + 2));
        } else {
            nodeFeatures.add("/");
        }
        // left1 + center
        if (index > 0) {
            nodeFeatures.add("c-1c." + example.get(index - 1) + "." + example.get(index));
        } else {
            nodeFeatures.add("/");
        }
        // center + right1
        if (index < example.size() - 1) {
            nodeFeatures.add("cc1." + example.get(index) + "." + example.get(index + 1));
        } else {
            nodeFeatures.add("/");
        }
        // left2 + left1
        if (index > 1) {
            nodeFeatures.add("c-2c-1." + example.get(index - 2) + "." + example.get(index - 1));
        } else {
            nodeFeatures.add("/");
        }
        // right1 + right2
        if (index < example.size() - 2) {
            nodeFeatures.add("c1c2." + example.get(index + 1) + "." + example.get(index + 2));
        } else {
            nodeFeatures.add("/");
        }
    }

    private void createWordBasedFeature(List<String> nodeFeatures, int index, List<String> example) {

    }

    public static void main(String[] args) {
        String trainFile = "data/pku_test_gold.utf8";
        FeatureExtractor featureExtractor = new FeatureExtractor(trainFile);
        featureExtractor.build();
    }
}
