package util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.reflections.Reflections;

import annotation.CustomComponent;

public class CustomInjector  {
	
	private Map<Class<?>, Class<?>> diMap;
	private Map<Class<?>, Object> applicationScope;
	
	private static CustomInjector injector;
	
	private CustomInjector() {
		super();
		diMap = new HashMap<Class<?>, Class<?>>();
		applicationScope = new HashMap<Class<?>, Object>();
	}
	
	public static void startApplication(Class<?> mainClass) {
		try {
			synchronized (CustomInjector.class) {
				if (injector == null) {
					injector = new CustomInjector();
					injector.initFramework(mainClass);
				}
				
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initFramework(Class<?> mainClass) throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		Class<?>[] classes = ClassLoaderUtil.getClasses(ClassLoaderUtil.getPackage(mainClass), mainClass);
		Reflections reflections = new Reflections(mainClass.getPackage().getName());
		Reflections reflections2 = new Reflections(ClassLoaderUtil.getPackage(mainClass));
		Set<Class<?>> types = reflections.getTypesAnnotatedWith(CustomComponent.class);
		Set<Class<?>> types2 = reflections2.getTypesAnnotatedWith(CustomComponent.class);
		types.addAll(types2);
		for (Class<?> implementationClass : types) {
			Class<?>[] interfaces = implementationClass.getInterfaces();
			if (interfaces.length == 0) {
				diMap.put(implementationClass, implementationClass);
				
			}
			else {
				for (Class<?> iface :interfaces) {
					diMap.put(implementationClass, iface);
				}
			}
		}
		
		for (Class<?> classz : classes) {
			if (classz.isAnnotationPresent(CustomComponent.class)) {
				Object classInstance = classz.newInstance();
				applicationScope.put(classz, classInstance);
				InjectionUtil.autowire(this, classz, classInstance);
			}
		}
	}
	
	public static <T> T getService(Class<T> classz) {
		try {
			return injector.getBeanInstance(classz);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private <T> T getBeanInstance(Class<T> interfaceClass) throws InstantiationException, IllegalAccessException {
		return (T) getBeanInstance(interfaceClass, null, null);
	}
	
	public <T> Object getBeanInstance(Class<T> interfaceClass, String fieldName, String qualifier) throws InstantiationException, IllegalAccessException {
		Class<?> implementationClass = getImplementationClass(interfaceClass, fieldName, qualifier);
		if (applicationScope.containsKey(implementationClass)) {
			return applicationScope.get(implementationClass);
		}
		
		synchronized (applicationScope) {
			Object service = implementationClass.newInstance();
			applicationScope.put(implementationClass, service);
			return service;
		}
		
	}
	
	private Class<?> getImplementationClass(Class<?> interfaceClass, final String fieldName, final String qualifier) {
		Set<Entry<Class<?>, Class<?>>> implementationClasses = diMap.entrySet().stream()
				.filter(entry -> entry.getValue() == interfaceClass).collect(Collectors.toSet());
		String errorMessage = "";
		if (implementationClasses == null || implementationClasses.size() == 0) {
			errorMessage = "No implementation found for " + interfaceClass.getName();
		}
		else if (implementationClasses.size() == 1) {
			Optional<Entry<Class<?>, Class<?>>> optional = implementationClasses.stream().findFirst();
			if (optional.isPresent()) {
				return optional.get().getKey();
			}
		}
		else if (implementationClasses.size() > 1) {
			final String findBy = (qualifier == null || qualifier.trim().length() == 0) ? fieldName : qualifier;
			Optional<Entry<Class<?>, Class<?>>> optional = implementationClasses.stream()
					.filter(entry -> entry.getKey().getSimpleName().equalsIgnoreCase(findBy)).findAny();
			if (optional.isPresent()) {
				return optional.get().getKey();
			}
			else {
				errorMessage = "There are " + implementationClasses.size() + " implementations of interface " +
			interfaceClass.getName() + ". Expected single implementation or make use of @CustomQualifier to resolve conflict";
			}
		}
		
		throw new RuntimeErrorException(new Error(errorMessage));
	}
	
	
	
}