package com.huaze.shen.feature;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Huaze Shen
 * @date 2019-04-19
 *
 * 提取特征
 */
public class FeatureExtractor {
    private String trainFile;
    public FeatureExtractor(String trainFile) {
        this.trainFile = trainFile;
    }

    public void build() {
        try {
            InputStream inputStream = FeatureExtractor.class.getClassLoader().getResourceAsStream(trainFile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                String[] lineSplit = line.trim().split(" ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String trainFile = "data/pku_test_gold.utf8";
        FeatureExtractor featureExtractor = new FeatureExtractor(trainFile);
        featureExtractor.build();
    }
}
