from gensim import interfaces
from gensim.corpora.dictionary import Dictionary
import json
from lxml import etree
import logging


def cleanup_etree(elem):
    elem.clear()
    while elem.getprevious() is not None:
        del elem.getparent()[0]


class DiceTopicModelingXmlCorpus(interfaces.CorpusABC):
    def __init__(self, fname, dictionary=None):
        self.fname = fname
        self.build_dictionary = False
        if dictionary is not None:
            self.dictionary = dictionary
        else:
            self.dictionary = Dictionary()
            self.init_dictionary()

    def init_dictionary(self):
        self.build_dictionary = True
        for document in self:
            self.dictionary.num_docs += 1
            self.dictionary.num_nnz += len(document)
            for word_id, freq in document:
                self.dictionary.cfs[word_id] = self.dictionary.cfs.get(word_id, 0) + freq
                self.dictionary.dfs[word_id] = self.dictionary.dfs.get(word_id, 0) + 1
                self.dictionary.num_pos += freq
        self.build_dictionary = False

    def prepare_document(self, keys=[], values=[], allocated=[], **kwargs):
        if self.dictionary.origid2id is not None:
            return list((self.dictionary.origid2id[k], v) for k, v, a in zip(keys, values, allocated) if a and k in self.dictionary.origid2id)
        else:
            return list((k, v) for k, v, a in zip(keys, values, allocated) if a)

    def __iter__(self):
        logging.debug('Reading documents: %s', self.fname)
        total = 0
        total_empty = 0
        for action, elem in etree.iterparse(self.fname, events=('end',)):
            localname = etree.QName(elem).localname
            if localname == 'Document':
                word_counts = elem.xpath('*[local-name()="DocumentWordCounts"]')
                assert len(word_counts) == 1
                document = self.prepare_document(**json.loads(word_counts[0].text))
                if len(document) != 0:
                    yield document
                    total += 1
                    if total % 10000 == 0:
                        logging.debug('Documents read: %d', total)
                else:
                    total_empty += 1
                cleanup_etree(elem)
            elif self.build_dictionary and localname == 'word':
                id = int(elem.get('id'))
                word = elem.text
                self.dictionary.id2token[id] = word
                self.dictionary.token2id[word] = id
                cleanup_etree(elem)
        logging.debug('Total documents: %d', total)
        logging.debug('Total documents discarded after filtering extremes: %d', total_empty)
        self.length = total

    def __len__(self):
        """Get the corpus size = the total number of documents in it."""
        if self.length is None:
            raise Exception("length is not ready")
        return self.length
