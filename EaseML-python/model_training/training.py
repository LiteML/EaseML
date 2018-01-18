from pydoc import locate
from mq import *
from sklearn.model_selection import train_test_split


def data_iterator(X, y, batch, epoch):
    eNum = 0
    while eNum < epoch:
        for i in xrange(0, len(X), batch):
            upper = min(batch + i, len(X))
            yield X[i:upper], y[i:upper]
        eNum += 1

class Training(object):
    def __init__(self, host, port, user, password, model_name, metric_func, queue_name, job_id, probability=True):
        self.publisher = MqPublisher(host, port, user, password)
        self.model = locate(model_name)()
        self.probability = probability
        self.job_id = job_id
        self.queue_name = queue_name
        self.iter = 0
        self.metric = locate(metric_func)
        self.metric_name = self.metric.__name__
        self.model_name = model_name

    def get_model(self):
        return self.model


class Sk_Training(Training):
    def __init__(self, host, port, user, password, model_name, metric_func, queue_name, job_id, batch, epoch, classes, probability=True):
        super(Sk_Training, self).__init__( host, port, user, password, model_name, metric_func, queue_name, job_id, probability)
        self.batch = batch
        self.epoch = epoch
        self.classes = classes

    def monitor(self, y_true, y_pred):
        ret = self.metric(y_true, y_pred)
        self.iter += 1
        metrics = {"id": self.job_id,
               "epoch":self.iter,
               "metrics": {self.metric_name: ret}}
        self.publisher.publish(self.queue_name, json.dumps(metrics))
        return self.metric_name, ret

    def classifier(self, X, y, **kwargs):
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        self.model.set_params(**kwargs)
        if self.probability:
            for batch_x, batch_y in data_iterator(X_train, y_train, self.batch, self.epoch):
                if self.iter == 0:
                    self.model.partial_fit(batch_x, batch_y, classes=range(self.classes))
                self.model.partial_fit(batch_x, batch_y)
                y_pred = self.model.predict_proba(X_test)
                self.monitor(y_test, y_pred)
        else:
            for batch_x, batch_y in data_iterator(X_train, y_train, self.batch, self.epoch):
                if self.iter == 0:
                    self.model.partial_fit(batch_x, batch_y, classes=range(self.classes))
                self.model.partial_fit(batch_x, batch_y)
                y_pred = self.model.predict(X_test)
                self.monitor(y_test, y_pred)
        self.model.partial_fit(X_test, y_test)

    def get_model(self):
        super(Sk_Training,self).get_model()


class GBDT_Training(Training):
    def __init__(self, host, port, user, password, model_name, metric_func, queue_name, job_id, probability=True):
        super(GBDT_Training, self).__init__(host, port, user, password, model_name, metric_func, queue_name, job_id, probability)

    def monitor(self, y1, y2, *args):
        if "xgboost" in self.model_name:
            if not self.probability:
                y1 = y1 > 0.5
            ret = self.metric(y2.get_label(), y1)
            self.iter += 1
            metrics = {"id": self.job_id,
               "epoch":self.iter,
               "metrics": {self.metric_name: ret}}
            self.publisher.publish(self.queue_name, json.dumps(metrics))
            return self.metric_name, ret
        else:
            if not self.probability:
                y2 = y2 > 0.5
            ret = self.metric(y1, y2)
            metrics = {"id": self.job_id,
                       "epoch":self.iter,
                       "metrics": {self.metric_name: ret}}
            self.publisher.publish(self.queue_name, json.dumps(metrics))
            return self.metric_name, ret, self.iter

    def classifier(self, X, y, **kwargs):
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        if "xgboost" in self.model_name:
            self.model.set_params(**kwargs)
            self.model.fit(X_train, y_train, eval_set=[(X_test, y_test)], eval_metric=self.monitor)
            self.model.fit(X_test, y_test)
        else:
            self.model.set_params(**kwargs)
            self.model.fit(X_train, y_train, eval_set=[(X_test, y_test)], eval_metric=self.monitor)

    def get_model(self):
        super(GBDT_Training, self).get_model()


if __name__ == '__main__':
    from sklearn.datasets import make_classification
    X, y = make_classification()
    train = GBDT_Training(host="localhost", port=5672, user="chris",
                     password="chris", model_name="lightgbm.LGBMClassifier",
                    metric_func="sklearn.metrics.f1_score",queue_name="metrics", job_id="sdf", probability=False)
    train.classifier(X, y)