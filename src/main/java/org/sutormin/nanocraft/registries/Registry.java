package org.sutormin.nanocraft.registries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Registry<T> {
    private List<T> things = new ArrayList<>();
    private Function<Integer, T> constructor;
    public Registry(Function<Integer, T> c){
        constructor = c;
    }
    public T add(){T t = constructor.apply(things.size()); things.add(t); return t;};
    public int toId(T thing){return things.indexOf(thing);};
    public T toThing(int id){return things.get(id);};
}
