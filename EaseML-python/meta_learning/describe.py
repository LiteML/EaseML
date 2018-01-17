# -*- coding: utf-8 -*-
"""
Created on Mon Dec  4 17:26:32 2017

@author: Yidan
"""
from __future__ import division
import numpy as np
import scipy as sp
import pandas as pd
from collections import defaultdict
import sklearn.discriminant_analysis
import sklearn.naive_bayes
import sklearn.tree
import sklearn.metrics
from sklearn.cross_validation import train_test_split

#%%
class To_Metadataset(object):
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.categorical = self.Creat_Categorical(x, y)
        
        #中间函数结果存储
        self.attribute_helper_MissingValues = self.helper_MissingValues(x, y, self.categorical)
        self.attribute_helper_ClassOccurences = self.helper_ClassOccurences(x, y, self.categorical)
        self.attribute_helper_NumSymbols = self.helper_NumSymbols(x, y, self.categorical)
        self.attribute_helper_Kurtosisses = self.helper_Kurtosisses(x, y, self.categorical)
        self.attribute_helper_Skewnesses = self.helper_Skewnesses(x, y, self.categorical)
        
    #阀值设定为10%，>10%为连续变量
    def Creat_Categorical(self, x, y):
        Categorical = []
        for i in range(x.shape[1]):
            if len(np.unique(x[:, i])) < (0.1* x.shape[0]):
                Categorical.append(1)
            else:
                Categorical.append(0)
        categorical = np.array(Categorical)
        return categorical
        
    def NumberOfInstances(self, X, y, categorical):
        return float(X.shape[0])
        
    def LogNumberOfInstances(self, X, y, categorical):
        return np.log(self.NumberOfInstances(X, y, categorical))
        
    #if len(y.shape) == 2并没有讨论
    def NumberOfClasses(self, X, y, categorical):
        return float(len(np.unique(y)))
    
    def NumberOfFeatures(self, X, y, categorical):
        return float(X.shape[1])
        
    def LogNumberOfFeatures(self, X, y, categorical):
        return np.log(self.NumberOfFeatures(X, y, categorical))
    
    ###################################################################################中间函数 1
    #没考虑输入为稀疏矩阵
    def helper_MissingValues(self, X, y, categorical):
        missing = ~np.isfinite(X)
        return missing
    
    #没考虑输入为稀疏矩阵
    def NumberOfInstancesWithMissingValues(self, X, y, categorical):
        missing = self.attribute_helper_MissingValues
        num_missing = missing.sum(axis=1)
        return float(np.sum([1 if num > 0 else 0 for num in num_missing]))
    
    def PercentageOfInstancesWithMissingValues(self, X, y, categorical):
        return float(self.NumberOfInstancesWithMissingValues(X, y, categorical) / X.shape[0])
    
    #没考虑输入为稀疏矩阵
    def NumberOfFeaturesWithMissingValues(self, X, y, categorical):
        missing = self.attribute_helper_MissingValues
        num_missing = missing.sum(axis=0)
        return float(np.sum([1 if num > 0 else 0 for num in num_missing]))        

    def PercentageOfFeaturesWithMissingValues(self, X, y, categorical):
        return float(self.NumberOfFeaturesWithMissingValues(X, y, categorical) / X.shape[1])
        
    def NumberOfMissingValues(self, X, y, categorical):
        return float(self.attribute_helper_MissingValues.sum())
    
    def PercentageOfMissingValues(self, X, y, categorical):
        return float(self.attribute_helper_MissingValues.sum() / (X.shape[0] * X.shape[1]))
    
    def NumberOfNumericFeatures(self, X, y, categorical):
        return len(categorical) - np.sum(categorical)
    
    def NumberOfCategoricalFeatures(self, X, y, categorical):
        return np.sum(categorical)
        
    def RatioNumericalToNominal(self, X, y, categorical):
        num_categorical =self.NumberOfCategoricalFeatures(X, y, categorical)
        num_numerical = self.NumberOfNumericFeatures(X, y, categorical)
        if num_categorical == 0:
            return 0.
        return float(num_numerical / num_categorical)
        
    def RatioNominalToNumerical(self, X, y, categorical):
        num_categorical =self.NumberOfCategoricalFeatures(X, y, categorical)
        num_numerical = self.NumberOfNumericFeatures(X, y, categorical)
        if num_numerical == 0:
            return 0.
        return float(num_categorical / num_numerical)
        
    def DatasetRatio(self, X, y, categorical):
        return float(X.shape[1] / X.shape[0])
        
    def LogDatasetRatio(self, X, y, categorical):
        return np.log(self.DatasetRatio(X, y, categorical))
        
    def InverseDatasetRatio(self, X, y, categorical):
        return float(X.shape[0] / X.shape[1])
    
    def LogInverseDatasetRatio(self, X, y, categorical):
        return np.log(self.InverseDatasetRatio(X, y, categorical))
        
    ###################################################################################中间函数 2
    #可以对比用 Counter(y) 的效率
    def helper_ClassOccurences(self, X, y, categorical):
        occurence_dict = defaultdict(float)
        for value in y:
            occurence_dict[value] += 1
        return occurence_dict
        
    #if len(y.shape) == 2并没有考虑
    def ClassProbabilityMin(self, X, y, categorical):
        occurences = self.attribute_helper_ClassOccurences
        min_value = np.iinfo(np.int64).max
        for num_occurences in occurences.values():
            if num_occurences < min_value:
                min_value = num_occurences
        return float(min_value) / y.shape[0]
    
    #if len(y.shape) == 2并没有考虑
    def ClassProbabilityMax(self, X, y, categorical):
        occurences = self.attribute_helper_ClassOccurences
        max_value = -1
        for num_occurences in occurences.values():
            if num_occurences > max_value:
                max_value = num_occurences
        return float(max_value) / y.shape[0]
        
    #if len(y.shape) == 2并没有考虑    
    def ClassProbabilityMean(self, X, y, categorical):
        occurence_dict = self.attribute_helper_ClassOccurences
        occurences = np.array([occurrence for occurrence in occurence_dict.values()],
                                  dtype=np.float64)
        return (occurences / y.shape[0]).mean()
        
    #if len(y.shape) == 2并没有考虑    
    def ClassProbabilitySTD(self, X, y, categorical):
        occurence_dict = self.attribute_helper_ClassOccurences
        occurences = np.array([occurrence for occurrence in occurence_dict.values()],
                                  dtype=np.float64)
        return (occurences / y.shape[0]).std()
        
    ###################################################################################中间函数 3
    #没考虑输入为稀疏矩阵
    def helper_NumSymbols(self, X, y, categorical):
        symbols_per_column = []
        for i, column in enumerate(X.T):
            if categorical[i]:
                unique_values = np.unique(column)
                num_unique = np.sum(np.isfinite(unique_values))
                symbols_per_column.append(num_unique)
        return symbols_per_column
        
    def SymbolsMin(self, X, y, categorical):
        minimum = None
        for unique in self.attribute_helper_NumSymbols:
            if unique > 0 and (minimum is None or unique < minimum):
                minimum = unique
        return minimum if minimum is not None else 0
    
    def SymbolsMax(self, X, y, categorical):
        values = self.attribute_helper_NumSymbols
        if len(values) == 0:
            return 0
        return max(max(values), 0)
        
    def SymbolsMean(self, X, y, categorical):
        values = [val for val in self.attribute_helper_NumSymbols if val > 0]
        mean = np.nanmean(values)
        return mean if np.isfinite(mean) else 0
        
    def SymbolsSTD(self, X, y, categorical):
        values = [val for val in self.attribute_helper_NumSymbols if val > 0]
        std = np.nanstd(values)
        return std if np.isfinite(std) else 0
        
    def SymbolsSum(self, X, y, categorical):
        sum_ = np.nansum(self.attribute_helper_NumSymbols)
        return sum_ if np.isfinite(sum_) else 0
    
    ###################################################################################中间函数 4
    #没考虑输入为稀疏化矩阵
    def helper_Kurtosisses(self, X, y, categorical):
        kurts = []
        for i in range(X.shape[1]):
            if not categorical[i]:
                kurts.append(sp.stats.kurtosis(X[:, i]))
        return kurts
        
    def KurtosisMin(self, X, y, categorical):
        kurts = self.attribute_helper_Kurtosisses
        minimum = np.nanmin(kurts) if len(kurts) > 0 else 0
        return minimum if np.isfinite(minimum) else 0
        
    def KurtosisMax(self, X, y, categorical):
        kurts = self.attribute_helper_Kurtosisses
        maximum = np.nanmax(kurts) if len(kurts) > 0 else 0
        return maximum if np.isfinite(maximum) else 0
        
    def KurtosisMean(self, X, y, categorical):
        kurts = self.attribute_helper_Kurtosisses
        mean = np.nanmean(kurts) if len(kurts) > 0 else 0
        return mean if np.isfinite(mean) else 0
        
    def KurtosisSTD(self, X, y, categorical):
        kurts = self.attribute_helper_Kurtosisses
        std = np.nanstd(kurts) if len(kurts) > 0 else 0
        return std if np.isfinite(std) else 0
        
    ###################################################################################中间函数 5
    #没考虑输入为稀疏化矩阵
    def helper_Skewnesses(self, X, y, categorical):
        skews = []
        for i in range(X.shape[1]):
            if not categorical[i]:
                skews.append(sp.stats.skew(X[:, i]))
        return skews
        
    def SkewnessMin(self, X, y,categorical):
        skews = self.attribute_helper_Skewnesses
        minimum = np.nanmin(skews) if len(skews) > 0 else 0
        return minimum if np.isfinite(minimum) else 0
        
    def SkewnessMax(self, X, y,categorical):
        skews = self.attribute_helper_Skewnesses
        maximum = np.nanmax(skews) if len(skews) > 0 else 0
        return maximum if np.isfinite(maximum) else 0
        
    def SkewnessMean(self, X, y,categorical):
        skews = self.attribute_helper_Skewnesses
        mean = np.nanmean(skews) if len(skews) > 0 else 0
        return mean if np.isfinite(mean) else 0
        
    def SkewnessSTD(self, X, y,categorical):
        skews = self.attribute_helper_Skewnesses
        std = np.nanstd(skews) if len(skews) > 0 else 0
        return std if np.isfinite(std) else 0
    
    #只考虑len(y.shape) == 1,如果考虑了>1，需要加if判断，最后取平均
    def ClassEntropy(self, X, y,categorical):
        y = y.reshape((-1, 1))
        occurence_dict = defaultdict(float)
        for value in y[:, 0]:
            occurence_dict[value] += 1
        entropies = sp.stats.entropy([val for val in occurence_dict.itervalues()], base=2)
        return entropies
    
    #没考虑交叉验证，也只考虑len(y.shape) == 1
    #选用acc做标准
    #抛除输入为稀疏化矩阵情况
    def LandmarkLDA(self, X, y,categorical):
        try:
            X_train,X_test,y_train,y_test = train_test_split(X, y, test_size=0.3)
            lda = sklearn.discriminant_analysis.LinearDiscriminantAnalysis()
            lda.fit(X_train, y_train)
            predictions = lda.predict(X_test)
            accuracy = sklearn.metrics.accuracy_score(predictions, y_test)
            return accuracy
            
        except:
            return np.nan

    #没考虑交叉验证，也只考虑len(y.shape) == 1
    #选用acc做标准
    #抛除输入为稀疏化矩阵情况            
    def LandmarkNaiveBayes(self, X, y,categorical):
        try:
            X_train,X_test,y_train,y_test = train_test_split(X, y, test_size=0.3)
            nb = sklearn.naive_bayes.GaussianNB()
            nb.fit(X_train, y_train)
            predictions = nb.predict(X_test)
            accuracy = sklearn.metrics.accuracy_score(predictions, y_test)
            return accuracy
            
        except:
            return np.nan

    #没考虑交叉验证，也只考虑len(y.shape) == 1
    #选用acc做标准
    #抛除输入为稀疏化矩阵情况            
    def LandmarkDecisionTree(self, X, y,categorical):
        try:
            X_train,X_test,y_train,y_test = train_test_split(X, y, test_size=0.3)
            random_state = sklearn.utils.check_random_state(42)
            tree = sklearn.tree.DecisionTreeClassifier(random_state=random_state)
            tree.fit(X_train, y_train)
            predictions = tree.predict(X_test)
            accuracy = sklearn.metrics.accuracy_score(predictions, y_test)
            return accuracy
            
        except:
            return np.nan

    #没考虑交叉验证，也只考虑len(y.shape) == 1
    #选用acc做标准
    #抛除输入为稀疏化矩阵情况    
    def LandmarkDecisionNodeLearner(self, X, y,categorical):
        try:
            X_train,X_test,y_train,y_test = train_test_split(X, y, test_size=0.3)
            random_state = sklearn.utils.check_random_state(42)
            node = sklearn.tree.DecisionTreeClassifier(criterion="entropy", max_depth=1, 
                                                       random_state=random_state,
                                                       min_samples_split=2, 
                                                       min_samples_leaf=1, 
                                                       max_features=None)
                                                       
            node.fit(X_train, y_train)
            predictions = node.predict(X_test)
            accuracy = sklearn.metrics.accuracy_score(predictions, y_test)
            return accuracy
            
        except:
            return np.nan    

    #没考虑交叉验证，也只考虑len(y.shape) == 1
    #选用acc做标准
    #抛除输入为稀疏化矩阵情况    
    def LandmarkRandomNodeLearner(self, X, y,categorical):
        try:
            X_train,X_test,y_train,y_test = train_test_split(X, y, test_size=0.3)
            random_state = sklearn.utils.check_random_state(42)
            node = sklearn.tree.DecisionTreeClassifier(criterion="entropy", max_depth=1, 
                                                       random_state=random_state,
                                                       min_samples_split=2, 
                                                       min_samples_leaf=1, 
                                                       max_features=1)
                                                       
            node.fit(X_train, y_train)
            predictions = node.predict(X_test)
            accuracy = sklearn.metrics.accuracy_score(predictions, y_test)
            return accuracy
            
        except:
            return np.nan        
    
    def run(self):
        def cover(func):
            return func(self.x, self.y, self.categorical)
            
        return (cover(self.NumberOfInstances), cover(self.LogNumberOfInstances), 
                cover(self.NumberOfClasses), cover(self.NumberOfFeatures), 
                cover(self.LogNumberOfFeatures), cover(self.NumberOfInstancesWithMissingValues), 
                cover(self.PercentageOfInstancesWithMissingValues), cover(self.NumberOfFeaturesWithMissingValues), 
                cover(self.PercentageOfFeaturesWithMissingValues), cover(self.NumberOfMissingValues), 
                cover(self.PercentageOfMissingValues), cover(self.NumberOfNumericFeatures), 
                cover(self.NumberOfCategoricalFeatures), cover(self.RatioNumericalToNominal), 
                cover(self.RatioNominalToNumerical), cover(self.DatasetRatio), 
                cover(self.LogDatasetRatio), cover(self.InverseDatasetRatio), 
                cover(self.LogInverseDatasetRatio), cover(self.ClassProbabilityMin), 
                cover(self.ClassProbabilityMax), cover(self.ClassProbabilityMean), 
                cover(self.ClassProbabilitySTD), cover(self.SymbolsMin), 
                cover(self.SymbolsMax), cover(self.SymbolsMean), 
                cover(self.SymbolsSTD), cover(self.SymbolsSum), 
                cover(self.KurtosisMin), cover(self.KurtosisMax), 
                cover(self.KurtosisMean), cover(self.KurtosisSTD), 
                cover(self.SkewnessMin), cover(self.SkewnessMax), 
                cover(self.SkewnessMean), cover(self.SkewnessSTD),
                cover(self.ClassEntropy), cover(self.LandmarkLDA), 
                cover(self.LandmarkNaiveBayes), cover(self.LandmarkDecisionTree), 
                cover(self.LandmarkDecisionNodeLearner), cover(self.LandmarkRandomNodeLearner))
                
#%%
if __name__ == '__main__':
    #导入数据， x，y————array
    x = pd.read_table('/Users/tend/Documents/auto-parameter/meta-learning/data.txt', sep = ' ')
    x = np.array(x)
    y = x[:, 0].astype(int)
    x = x[:,1:]
    to_metadataset = To_Metadataset(x, y)
    res = to_metadataset.run()
    print res
