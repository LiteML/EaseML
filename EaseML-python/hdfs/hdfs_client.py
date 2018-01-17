# -*- coding: utf-8 -*-
from hdfs.client import InsecureClient
import pandas as pd
import pickle

class hdfs_client(object):
    ## the hdfs_client for save and data from hdfs

    def __init__(self, url, user):
        self.client = InsecureClient(url, user)

    def write_file(self, data, path):
        with self.client.write(path, encoding='utf-8', overwrite=True) as writer:
            data.to_csv(writer, index=False)

    def read_file(self, path):
        with self.client.read(path, encoding='utf-8') as reader:
            data = pd.read_csv(reader, header=True)
            return data

    def write_obj(self, obj, path):
        with self.client.write(path, overwrite=True) as writer:
            pickle.dump(obj, writer)

    def read_obj(self, path):
        with self.client.read(path) as reader:
            obj = pickle.load(reader)
            return obj












