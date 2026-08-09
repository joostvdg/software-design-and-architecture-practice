package com.isaqb.practice.pipesandfilters;


import java.util.List;

@FunctionalInterface
public interface Filter<I, O> {

    List<O> apply(List<I> input);
}