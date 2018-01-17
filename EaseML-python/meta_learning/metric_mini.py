# -*- coding: utf-8 -*-
"""
Created on Tue Dec 26 11:33:15 2017
@author: Yidan
"""
#%%
import numpy as np
import scipy as sp
from scipy import spatial
import pandas as pd
from sklearn import metrics
from collections import defaultdict
import json
import math

##########################################     input: 1. meta-feature     2. 度量方法     3. 需要返回的初始化参数套数k     4. 用的分类方法
##########################################     output: 返回k套初始化参数
#%%
class To_Hyperparameters(object):
    def __init__(self, metafeature, metric, k, func):
        self.metafeature = metafeature
        self.metric = metric
        self.k = k
        self.func = func
        #库
        self.fea_library = self.feature_library()
        self.heper_library = self.Hyperpara_library()
        
    def feature_library(self):
        library = pd.read_table('/Users/tend/Documents/auto-parameter/meta-learning/meta_library.txt', sep = ',')
        used_feature = ['NumberOfInstances', 
                        'LogNumberOfInstances', 
                        'NumberOfClasses', 
                        'NumberOfFeatures', 
                        'LogNumberOfFeatures', 
                        'NumberOfInstancesWithMissingValues', 
                        'PercentageOfInstancesWithMissingValues', 
                        'NumberOfFeaturesWithMissingValues', 
                        'PercentageOfFeaturesWithMissingValues', 
                        'NumberOfMissingValues', 
                        'PercentageOfMissingValues', 
                        'NumberOfNumericFeatures', 
                        'NumberOfCategoricalFeatures', 
                        'RatioNumericalToNominal', 
                        'RatioNominalToNumerical', 
                        'DatasetRatio', 
                        'LogDatasetRatio', 
                        'InverseDatasetRatio', 
                        'LogInverseDatasetRatio', 
                        'ClassProbabilityMin', 
                        'ClassProbabilityMax', 
                        'ClassProbabilityMean', 
                        'ClassProbabilitySTD', 
                        'SymbolsMin', 
                        'SymbolsMax', 
                        'SymbolsMean', 
                        'SymbolsSTD', 
                        'SymbolsSum', 
                        'KurtosisMin', 
                        'KurtosisMax', 
                        'KurtosisMean', 
                        'KurtosisSTD', 
                        'SkewnessMin', 
                        'SkewnessMax', 
                        'SkewnessMean', 
                        'SkewnessSTD', 
                        'ClassEntropy', 
                        'LandmarkLDA', 
                        'LandmarkNaiveBayes', 
                        'LandmarkDecisionTree', 
                        'LandmarkDecisionNodeLearner', 
                        'LandmarkRandomNodeLearner']
                        
        used_library = library[used_feature]
        used_library.index = library.instance_id
        used_library = used_library.replace('?', '0')
        
        return used_library
        
    #导入参数库
    def Hyperpara_library(self):
        acc_library = pd.read_table('/Users/tend/Documents/auto-parameter/meta-learning/hyper_acc_library.txt', sep = ',')
        acc_index = pd.read_table('/Users/tend/Documents/auto-parameter/meta-learning/hyper_acc_algorithm.txt', sep = ',')
        raw_acc_library = pd.concat([acc_index, acc_library], axis = 1)
        
        return raw_acc_library
    
    #分组
    def grouping(self):
        ##### 各分类器分组 #####
        class_1 = ['classifier:adaboost', 
                   'classifier:gradient_boosting', 
                   'classifier:extra_tree', 
                   'classifier:random_forest']
                   
        class_2 = ['classifier:liblinear_svc', 
                   'classifier:libsvm_svc', 
                   'classifier:passive_aggressive']
                   
        class_3 = ['classifier:bernoulli_nb', 
                   'classifier:multinomial_nb']
                   
        class_4 = ['classifier:lda', 
                   'classifier:qda']
                   
        class_5 = ['classifier:sgd']
        
        class_6 = ['classifier:k_nearest_neighbors']
        
        ##### 分类器归类字典 #####
        name = ['boost', 'linear', 'nb', 'lda', 'sgd', 'knn']
        self.classifier_to_dict = dict(zip(name, [class_1, class_2, class_3, class_4, class_5, class_6]))
        
        ##### 各分类器所选参数 #####
        self.adaboost = ['classifier:adaboost:learning_rate',
                         'classifier:adaboost:max_depth', 
                         'classifier:adaboost:n_estimators']
                         
        self.gradient_boosting = ['classifier:gradient_boosting:learning_rate', 
                                  'classifier:gradient_boosting:max_depth',
                                  'classifier:gradient_boosting:n_estimators']
                                  
        self.extra_tree = ['classifier:extra_tree:criterion',
                           'classifier:extra_tree:max_depth',
                           'classifier:extra_tree:n_estimators']
                           
        self.random_forest = ['classifier:random_forest:criterion', 
                              'classifier:random_forest:max_depth', 
                              'classifier:random_forest:n_estimators',
                              'classifier:random_forest:bootstrap']
        
        self.liblinear_svc = ['classifier:liblinear_svc:C', 
                              'classifier:liblinear_svc:dual', 
                              'classifier:liblinear_svc:penalty', 
                              'classifier:liblinear_svc:tol']
                              
        self.libsvm_svc = ['classifier:libsvm_svc:C', 
                           'classifier:libsvm_svc:coef0', 
                           'classifier:libsvm_svc:degree', 
                           'classifier:libsvm_svc:gamma', 
                           'classifier:libsvm_svc:kernel', 
                           'classifier:libsvm_svc:shrinking', 
                           'classifier:libsvm_svc:tol']
                           
        self.passive_aggressive = ['classifier:passive_aggressive:C', 
                                   'classifier:passive_aggressive:n_iter']
                                   
        self.bernoulli_nb = ['classifier:bernoulli_nb:fit_prior']
                             
        self.multinomial_nb = ['classifier:multinomial_nb:fit_prior']
        
        self.lda = ['classifier:lda:tol']
        
        self.qda = ['classifier:qda:reg_param']
        
        self.sgd = ['classifier:sgd:alpha', 
                    'classifier:sgd:average', 
                    'classifier:sgd:eta0', 
                    'classifier:sgd:learning_rate', 
                    'classifier:sgd:loss', 
                    'classifier:sgd:n_iter', 
                    'classifier:sgd:penalty', 
                    'classifier:sgd:power_t']
                    
        self.k_nearest_neighbors = ['classifier:k_nearest_neighbors:n_neighbors', 
                                    'classifier:k_nearest_neighbors:p', 
                                    'classifier:k_nearest_neighbors:weights']
                                    
        ##### 各类分类器参数合并 #####
        para_1 = self.adaboost + self.gradient_boosting + self.extra_tree + self.random_forest
        para_2 = self.liblinear_svc + self.libsvm_svc + self.passive_aggressive
        para_3 = self.bernoulli_nb + self.multinomial_nb
        para_4 = self.lda + self.qda
        para_5 = self.sgd
        para_6 = self.k_nearest_neighbors
        
        self.para_to_dict = dict(zip(name, [para_1, para_2, para_3, para_4, para_5, para_6]))

    #根据指定分类，筛选出可以迁移的源域
    def Filter_source_domain(self):
        try:
            for key, val in self.classifier_to_dict.iteritems():
                if 'classifier:' + self.func in val:
                    class_name = key
        except:
            print 'Classifier out of range !'
        
        used_para_name = self.para_to_dict[class_name]
        para_data_df = self.heper_library.ix[:, used_para_name]
        para_data_df.index = self.heper_library.ix[:, 'instance_id']
        self.para_data_df = para_data_df.dropna(how = 'all')

        return self.para_data_df
        
    #计算目标域meta-feature与源域meta-feature距离，返回距离字典
    def loop(self, computation_func):
        source_domain = self.fea_library.ix[self.para_data_df.index, :]
        self.source_domain_index = source_domain.index
        library = source_domain.applymap(lambda x: float(x))
        library = np.array(library)
        
        distance = defaultdict(float)
        for index, val in enumerate(library):
            if computation_func == 'cosine':
                distance[index] = self.cosine(self.metafeature, val)

            if computation_func == 'euclidean':
                distance[index] = self.euclidean(self.metafeature, val)
                
            if computation_func == 'seuclidean':
                distance[index] = self.seuclidean(self.metafeature, val)
                
            if computation_func == 'sqeuclidean':
                distance[index] = self.sqeuclidean(self.metafeature, val)
                
            if computation_func == 'Mahalanobis':
                distance[index] = self.Mahalanobis(self.metafeature, val)
                
            if computation_func == 'l1':
                distance[index] = self.l1_dis(self.metafeature, val)
                
            if computation_func == 'l2':
                distance[index] = self.l2(self.metafeature, val)
                
            if computation_func == 'Chebyshev':
                distance[index] = self.Chebyshev(self.metafeature, val)
                
            if computation_func == 'correlation':
                distance[index] = self.correlation(self.metafeature, val)
                
            ##应该设定一个最大容错率
                
        return distance
    
    #返回初始化的点的序号
    def to_initplot(self):
        distance = self.loop(self.metric)
        match = zip(self.source_domain_index, distance.values())
        sort_match = sorted([i for i in match], key = lambda x: x[1])
        inx = map(lambda x: x[0], sort_match)

        return inx[:self.k]

    #确定各类的默认值，为以后空缺值变成默认值作准备
    def default(self, func):
        if func == 'adaboost':
            next_x_default = {u'learning_rate': 0.1,
                              u'max_depth': 3,
                              u'n_estimators': 50}

        if func == 'gradient_boosting':
            next_x_default = {u'learning_rate': 0.1,
                              u'max_depth': 3,
                              u'n_estimators': 50}
                              
        if func == 'extra_tree':
            next_x_default = {u'criterion': 'gini',
                              u'max_depth': 3,
                              u'n_estimators': 10}

        if func == 'random_forest':
            next_x_default = {u'criterion': 'gini',
                              u'max_depth': 3,
                              u'n_estimators': 10, 
                              u'bootstrap': True}

        if func == 'k_nearest_neighbors':
            next_x_default = {u'n_neighbors': 5,
                              u'p': 2,
                              u'weights': 'uniform'}

        if func == 'liblinear_svc':
            next_x_default = {u'C': 1.0,
                              u'dual': True,
                              u'penalty': 'l2', 
                              u'tol': 1e-4}

        if func == 'libsvm_svc':
            next_x_default = {u'C': 1.0,
                              u'coef0': 0.0,
                              u'degree': 3, 
                              u'gamma': 0.0,
                              u'kernel': 'rbf', 
                              u'shrinking': True,
                              u'tol': 1e-3}

        if func == 'passive_aggressive':
            next_x_default = {u'C': 1.0,
                              u'n_iter': 5}

        if func == 'bernoulli_nb':
            next_x_default = {u'fit_prior': True}

        if func == 'multinomial_nb':
            next_x_default = {u'fit_prior': True}

        if func == 'lda':
            next_x_default = {u'tol': 0.0001}

        if func == 'qda':
            next_x_default = {u'reg_param': 0.0}

        if func == 'sgd':
            next_x_default = {u'alpha': 0.0001,
                              u'average': False,
                              u'eta0': 0.0, 
                              u'learning_rate': 'optimal', 
                              u'loss': 'hinge', 
                              u'n_iter': 5, 
                              u'penalty': 'l2', 
                              u'power_t': 0.5}
        return next_x_default

    def neat(self, dicts):
        if self.func == 'adaboost':
            if isinstance(dicts[u'max_depth'], float) and dicts[u'max_depth'] is not np.nan:
                dicts[u'max_depth'] = int(dicts[u'max_depth'])
            if isinstance(dicts[u'n_estimators'], float) and dicts[u'n_estimators'] is not np.nan:
                dicts[u'n_estimators'] = int(dicts[u'n_estimators'])
                
        if self.func == 'GradientBoostingClassifier':
            if isinstance(dicts[u'max_depth'], float) and dicts[u'max_depth'] is not np.nan:
                dicts[u'max_depth'] = int(dicts[u'max_depth'])
            if isinstance(dicts[u'n_estimators'], float) and dicts[u'n_estimators'] is not np.nan:
                dicts[u'n_estimators'] = int(dicts[u'n_estimators'])

        if self.func == 'extra_tree':
            if isinstance(dicts[u'max_depth'], float) and dicts[u'max_depth'] is not np.nan:
                dicts[u'max_depth'] = int(dicts[u'max_depth'])
            if isinstance(dicts[u'n_estimators'], float) and dicts[u'n_estimators'] is not np.nan:
                dicts[u'n_estimators'] = int(dicts[u'n_estimators'])
                
        if self.func == 'random_forest':
            if isinstance(dicts[u'max_depth'], float) and dicts[u'max_depth'] is not np.nan:
                dicts[u'max_depth'] = int(dicts[u'max_depth'])
            if isinstance(dicts[u'n_estimators'], float) and dicts[u'n_estimators'] is not np.nan:
                dicts[u'n_estimators'] = int(dicts[u'n_estimators'])
        
        if self.func == 'k_nearest_neighbors':
            if isinstance(dicts[u'n_neighbors'], float) and dicts[u'n_neighbors'] is not np.nan:
                dicts[u'n_neighbors'] = int(dicts[u'n_neighbors'])
            if isinstance(dicts[u'p'], float) and dicts[u'p'] is not np.nan:
                dicts[u'p'] = int(dicts[u'p'])
                
        if self.func == 'libsvm_svc':
            if isinstance(dicts[u'degree'], float) and dicts[u'degree'] is not np.nan:
                dicts[u'degree'] = int(dicts[u'degree'])

        if self.func == 'passive_aggressive':
            if isinstance(dicts[u'n_iter'], float) and dicts[u'n_iter'] is not np.nan:
                dicts[u'n_iter'] = int(dicts[u'n_iter'])

        if self.func == 'sgd':
            if isinstance(dicts[u'n_iter'], float) and dicts[u'n_iter'] is not np.nan:
                dicts[u'n_iter'] = int(dicts[u'n_iter'])
                
        return dicts
        
    def transform_para(self, func):
        top_meta = self.to_initplot()
        para_used_df = self.para_data_df.ix[top_meta, :]
        
        used_para = getattr(self, func)
        pure_para = map(lambda x: x.split(':')[-1], used_para)
        
        lists = []
        for ind, val in para_used_df.iterrows():
            next_x_default = self.default(self.func)
            val = val.dropna()
            val.index = map(lambda x: x.split(':')[-1], val.index)
            dicts = {}
            for i in pure_para:
                try:
                    dicts[i] = val[i]
                except:
                    dicts[i] = np.nan
            
                        
            dicts = self.neat(dicts)
            
            for para_i in dicts:
                if isinstance(dicts[para_i], float):
                    if not math.isnan(dicts[para_i]):
                        next_x_default[para_i] = dicts[para_i]

            if func == 'adaboost':
                next_x_default['loss'] = 'exponential'
                
            lists.append(next_x_default)
            self.res = lists
            
        lists = list(np.unique(lists))
        with open('./High_quality_para.json', 'w') as json_file:
            json_file.write(json.dumps(lists))
            
    #各距离算法
    def cosine(self, x1, x2):
        return spatial.distance.cosine(x1, x2)
        
    def euclidean(self, x1, x2):
        return spatial.distance.euclidean(x1, x2)
        
    def seuclidean(self, x1, x2):
        return spatial.distance.seuclidean(x1, x2)
        
    def sqeuclidean(self, x1, x2):
        return spatial.distance.sqeuclidean(x1, x2)
        
    def Mahalanobis(self, x1, x2):
        return spatial.distance.mahalanobis(x1, x2)
        
    def l1_dis(self, x1, x2):
        return metrics.pairwise.paired_distances(x1, x2, metric = 'l1')
        
    def l2_dis(self, x1, x2):
        return metrics.pairwise.paired_distances(x1, x2, metric = 'l2')   
        
    def Chebyshev(self, x1, x2):
        return spatial.distance.chebyshev(x1, x2)
        
    def correlation(self, x1, x2):
        return spatial.distance.correlation(x1, x2)
    
    def run(self):
        self.grouping()
        self.Filter_source_domain()
        self.transform_para(self.func)
        
#%%
if __name__ == '__main__':
    #导入数据
    res = (7290.0, 8.8942588250027033,10.0,256.0,5.5451774444795623,0.0,0.0,0.0,0.0,0.0,0.0,195,61,3.19672131147541,0.3128205128205128,
           0.035116598079561044,-3.3490813805231414,28.4765625,3.3490813805231414,0.07434842249657064,0.16378600823045267,0.10000000000000001,
           0.026982205708288898,63,710,439.93442622950818,196.76813923527712,26836,-1.8271334428574117,12.64619511441683,-0.23515023594575765,
           2.3139420510496485,-1.1694182419743819,3.6113187006165623,0.86490066747523842,0.83212748523566049,3.2743168766816022,0.91632373113854593,
           0.7540009144947416,0.87059899405578423,0.30086877000457246,0.16598079561042525)
    
    dic = To_Hyperparameters(res, 'cosine', 10, 'adaboost')
    dic.run()
    
    
    
