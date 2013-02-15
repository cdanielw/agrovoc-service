package agrovoc.util.config
/**
 * @author Daniel Wiell
 */
class Resources {
    private final Map<Class, Object> resources = [:]

    public <T> T getAt(Class<T> type) {
        def resource = resources[type]
        if (resource == null)
            assert resource, "Not configured: ${type}. Available: ${this.resources.keySet()}"
        return resource as T
    }

    public <T> void putAt(Class<? extends T> type, T resource) {
        assert resource, "Resource is null"
        if (type != resource.class)
            resources[resource.class] = resource
        resources[type] = resource
    }

    public <T> void putAt(List<Class<? extends T>> types, T resource) {
        assert resource, "Resource is null"
        types.each { putAt(it, resource) }
    }

    void add(Object resource) {
        assert resource, "Resource is null"
        putAt(resource.class, resource)
    }

    void addAll(Collection resources) {
        resources.each { add(it) }
    }

    void leftShift(Object resource) {
        add(resource)
    }

    Set<Object> getInstances() {
        resources.values()
    }
}
