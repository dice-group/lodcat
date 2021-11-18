from gensim import interfaces
from gensim.corpora.dictionary import Dictionary
import json
from lxml import etree


def prepare_document(keys=[], values=[], allocated=[], **kwargs):
    return list((k, v) for k, v, a in zip(keys, values, allocated) if a)


def cleanup_etree(elem):
    elem.clear()
    while elem.getprevious() is not None:
        del elem.getparent()[0]


class DiceTopicModelingXmlCorpus(interfaces.CorpusABC):
    def __init__(self, fname, dictionary=None):
        self.fname = fname
        self.id2word = {}
        self.dictionary = dictionary if dictionary is not None else Dictionary()

    def init_dictionary(self):
        for document in self:
            self.dictionary.num_docs += 1
            self.dictionary.num_nnz += len(document)
            for word_id, freq in document:
                self.dictionary.cfs[word_id] = self.dictionary.cfs.get(word_id, 0) + freq
                self.dictionary.dfs[word_id] = self.dictionary.dfs.get(word_id, 0) + 1
                self.dictionary.num_pos += freq

    def __iter__(self):
        total = 0
        for action, elem in etree.iterparse(self.fname, events=('end',)):
            localname = etree.QName(elem).localname
            if localname == 'Document':
                word_counts = elem.xpath('*[local-name()="DocumentWordCounts"]')
                assert len(word_counts) == 1
                yield prepare_document(**json.loads(word_counts[0].text))
                cleanup_etree(elem)
            elif localname == 'word':
                id = int(elem.get('id'))
                word = elem.text
                self.dictionary.id2token[id] = word
                self.dictionary.token2id[word] = id
                cleanup_etree(elem)
        self.length = total
