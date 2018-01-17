import pika
import json
import multiprocessing as mp

class MqBase(object):
    def __init__(self, host, port, user, password):
        self.connection = pika.BlockingConnection(pika.ConnectionParameters(host=host, port=port, credentials=pika.PlainCredentials(user, password)))
        self.channel = self.connection.channel()
        self.channel.basic_qos(prefetch_count=1)


    def declare_queue(self, queue_name):
        self.channel.queue_declare(queue_name, durable=True, exclusive=False, auto_delete=False)

    def delete_queue(self, queue_name):
        self.channel.queue_delete(queue_name)

    def close(self):
        self.channel.close()
        self.connection.close()

    def _publish(self, queue_name, msg):
        self.channel.basic_publish(exchange='', routing_key=queue_name,
                                   properties=pika.BasicProperties(content_type="application/json",
                                                                   delivery_mode=2), body=msg)

    def _consume(self, queue_name, callback, n_jobs, requeue=True):
        def on_request(ch, method, props, body):
            try:
                callback(json.loads(body))
                ch.basic_ack(delivery_tag=method.delivery_tag)
            except:
                if(requeue):
                    self._publish(queue_name, body)
                ch.basic_ack(delivery_tag=method.delivery_tag)
        def basic_consume():
            self.channel.basic_consume(on_request, no_ack=False, exclusive=False, queue=queue_name)

        workers = [mp.Process(target=basic_consume,args=()) for _ in range(n_jobs)]
        for w in workers:
            w.start()
        for w in workers:
            w.join()

class MqConsumer(MqBase):
    def __init__(self, host, port, user, password):
        super(MqConsumer, self).__init__(host, port, user, password)

    def consume(self, queue_name, callback, jobs, requeue=True):
        super(MqConsumer, self)._consume(queue_name, callback, jobs, requeue)



class MqPublisher(MqBase):
    def __init__(self, host, port, user, password):
        super(MqPublisher, self).__init__(host, port, user, password)

    def publish(self, queue_name, msg):
        super(MqPublisher, self)._publish(queue_name, msg)
















