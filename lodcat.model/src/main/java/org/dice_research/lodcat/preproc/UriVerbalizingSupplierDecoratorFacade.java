package org.dice_research.lodcat.preproc;

import java.util.*;
import org.dice_research.topicmodeling.preprocessing.docsupplier.DocumentSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UriVerbalizingSupplierDecoratorFacade extends AbstractUriVerbalizingSupplierDecorator {
    private static final Logger LOGGER = LoggerFactory.getLogger(UriVerbalizingSupplierDecoratorFacade.class);

    private List<AbstractUriVerbalizingSupplierDecorator> verbalizers;

    public UriVerbalizingSupplierDecoratorFacade(DocumentSupplier documentSource, AbstractUriVerbalizingSupplierDecorator... verbalizers) {
        this(documentSource, Arrays.asList(verbalizers));
    }

    public UriVerbalizingSupplierDecoratorFacade(DocumentSupplier documentSource, List<AbstractUriVerbalizingSupplierDecorator> verbalizers) {
        super(documentSource);
        this.verbalizers = verbalizers;
    }

    @Override
    protected String[] verbalizeUri(String uri) {
        String[] verbalizations = null;
        for (AbstractUriVerbalizingSupplierDecorator verbalizer : verbalizers) {
            verbalizations = verbalizer.verbalizeUri(uri);
            if (verbalizations != null && verbalizations.length != 0) {
                break;
            }
        }
        return verbalizations;
    }
}
