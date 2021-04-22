package org.ros.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Jonathan Groff Copyright (C) NeoCoreTechs 2015,2021
 */
public class CollectionMath {
  
  private CollectionMath() {
    // Utility class.
  }
 
  public static <T extends Comparable<? super T>> T median(Collection<T> collection) {
    assert(collection.size() > 0);
    List<T> list = new ArrayList<T>(collection);
    Collections.sort(list);
    return list.get(list.size() / 2);
  }
}
