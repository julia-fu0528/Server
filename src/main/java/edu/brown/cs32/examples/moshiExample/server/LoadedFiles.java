package edu.brown.cs32.examples.moshiExample.server;

import Exceptions.NoFileStoredException;

public class LoadedFiles<T>{
    public T storage;
    public LoadedFiles(){

    }
    public T getFile() throws NoFileStoredException {
        if (storage == null){
            throw new NoFileStoredException("No CSV loaded yet.");
        }
        return this.storage;
    }
    public void storeFile(T file){
        this.storage = file;
    }
}

