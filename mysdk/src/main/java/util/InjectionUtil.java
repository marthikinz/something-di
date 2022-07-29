package util;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import annotation.CustomAutowired;
import annotation.CustomQualifier;

public class InjectionUtil {
	private InjectionUtil() {
		super();
	}
	
	public static void autowire(CustomInjector injector, Class<?> classz, Object classInstance) throws InstantiationException, IllegalAccessException {
		Set<Field> fields = findFields(classz);
		for (Field field : fields) {
			String qualifier = field.isAnnotationPresent(CustomQualifier.class) 
					? field.getAnnotation(CustomQualifier.class).value()
							: null;
			Object fieldInstance = injector.getBeanInstance(field.getType(), field.getName(), qualifier);
			field.set(classInstance, fieldInstance);
			autowire(injector, fieldInstance.getClass(), fieldInstance);
		}
	}
	
	private static Set<Field> findFields(Class<?> classz) {
		Set<Field> set  = new HashSet<>();
		while (classz != null) {
			for (Field field : classz.getDeclaredFields()) {
				if (field.isAnnotationPresent(CustomAutowired.class)) {
					field.setAccessible(true);
					set.add(field);
				}
			}
			classz = classz.getSuperclass();
		}
		return set;
	}
}
