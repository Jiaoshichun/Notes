package com.example.demo.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Demo {
    public static void main(String[] args) {
findSubstring("wordgoodgoodgoodbestword",new String[]{"word","good","best","word"});
    }

    public static List<Integer> findSubstring(String s, String[] words) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(words));
            StringBuilder builder = new StringBuilder();
            connectStr(result, builder,arrayList);
        }
        int size = result.size();
        return null;
    }

    private static void connectStr(ArrayList<String> result, StringBuilder stringBuilder, ArrayList<String> arrayList) {
        if (arrayList.size()==0) {
            result.add(stringBuilder.toString());
            return;
        }
        for (int i = arrayList.size() - 1; i > -1; i--) {
            stringBuilder.append(arrayList.get(i));
            arrayList.remove(i);
            connectStr(result, stringBuilder, new ArrayList<>(arrayList));
        }
    }
}
