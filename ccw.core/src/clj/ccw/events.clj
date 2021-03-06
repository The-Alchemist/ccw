(ns ccw.events)

(try
  (require '[ccw.e4.model])
  (catch Exception e
    (println "ccw.e4.model could not be loaded.")))

;; remove this once CCW has definitely dropped Eclipse 3.x support
(def DATA
  "IEventBroker/DATA"
  "org.eclipse.e4.data")

(def event-broker
  (delay
    (try 
      (ccw.e4.model/context-key @ccw.e4.model/app :event-broker)
      (catch Exception e
        (println "Event Broker cannot be loaded. Please upgrade to Eclipse 4")
        nil))))

(defn as-topic [t]
  (cond
    (instance? String t) t
    (keyword? t) (-> t name (.replaceAll "\\." "/"))))

(defn- topic-as-keyword [t]
  (-> t (.replaceAll "\\/" ".") keyword))

(defn send-event [topic data]
  (when-let [event-broker @event-broker]
    (println "will send" (as-topic topic) ", data:" data)
    ;; we force the data type to smth other than a Map so
    ;; we get consistent behavior in the handler
    (.send event-broker (as-topic topic) [data])))

(defn post-event [topic data]
  (println "will post" (as-topic topic) ", data:" data)
  (when-let [event-broker @event-broker]
    ;; we force the data type to smth other than a Map so
    ;; we get consistent behavior in the handler
    (.post event-broker (as-topic topic) [data])))

(defn subscribe
  ([topic event-handler-var] (subscribe topic false event-handler-var))
  ([topic require-ui? event-handler-var]
    (println "subscribing to topic" (as-topic topic))
    (when-let [event-broker @event-broker]
      (let [event-handler
            (reify org.osgi.service.event.EventHandler
              (handleEvent [this event]
                (event-handler-var
                  (topic-as-keyword (.getTopic event))
                  ;; call to (get 0) to extract the data from the
                  ;; vector
                  (first (.getProperty event DATA)))))]
        (when (.subscribe event-broker
                (as-topic topic)
                nil
                event-handler
                (boolean require-ui?))
          event-handler)))))

(defn unsubscribe [event-handler]
  (when-let [event-broker @event-broker]
    (.unsubscribe event-broker
      event-handler)))
