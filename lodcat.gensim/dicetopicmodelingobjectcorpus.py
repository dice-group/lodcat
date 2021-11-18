from gensim import interfaces
from gensim.corpora.dictionary import Dictionary
import gzip
import javaobj.v2 as javaobj
import json


def find_field(obj, field_name):
    for cd, fields in reversed([_ for _ in obj.field_data.items()]):  # FIXME
        # if cd.name == class_name:
            for fd, data in fields.items():
                if fd.name == field_name:
                    return data


def find_annotations(obj, class_name):
    for cd, annotations in obj.annotations.items():
        if cd.name == class_name:
            return annotations


def map_entries(map, class_name="java.util.HashMap"):
    return zip(*([iter(find_annotations(map, class_name)[1:])] * 2))


def find_property(obj, class_name):
    for key, value in map_entries(obj):
        if key.name == class_name:
            return value


def prepare_document(keys=[], values=[], allocated=[], assigned=0, loadFactor=1, empty=False):
    return list((k, v) for k, v, a in zip(keys, values, allocated) if a)


class DiceTopicModelingObjectCorpus(interfaces.CorpusABC):
    def __init__(self, fname):
        with gzip.open(fname, 'rb') as f:
            self.corpus_object = javaobj.load(f)
        properties = find_field(self.corpus_object, "properties")
        corpus_vocab_p = find_property(properties, "org.dice_research.topicmodeling.utils.corpus.properties.CorpusVocabulary")
        value = find_field(corpus_vocab_p, "value")
        word_index_map = find_field(value, "wordIndexMap")
        id2word = dict((int(id), str(word)) for word, id in map_entries(word_index_map))
        self.dictionary = Dictionary.from_corpus(self, id2word=id2word)

    def __iter__(self):
        documents = find_field(self.corpus_object, "corpus")
        self.length = len(documents)
        for document in documents:
            properties = find_field(document, "properties")
            word_counts_p = find_property(properties, "org.dice_research.topicmodeling.utils.doc.DocumentWordCounts")
            word_counts = find_annotations(word_counts_p, "org.dice_research.topicmodeling.utils.doc.DocumentWordCounts")
            yield(prepare_document(**json.loads(str(word_counts[0]))))
