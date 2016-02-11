package net.demilich.metastone.game.behaviour.mcts.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Created by Georgios on 09/02/2016.
 */
public class ListUtils {

    private final static Logger logger = LoggerFactory.getLogger(ListUtils.class);


    public static <E> E getRandomElement(List<E> list) {
        if (list.size() == 0) return null;
        //logger.info("Selecting random element from {} with size {}", list, list.size());
        return list.get(new Random().nextInt(list.size()));
    }


    private ListUtils() {

    }
}
