package com.GenStack.kvhandler;

import org.w3c.dom.Element;

public interface KVSubjectHandlerInterface {
    void addKVSubject(String nameSpace, String storage, Element subjectElement);
    void removeKVSubject(String nameSpace, String storage, String identifier);
    KVSubject getKVSubject(String nameSpace, String storage, String identifier);
}
