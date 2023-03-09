package space.gavinklfong.demo.streamapi;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.h2.expression.BinaryOperation;
import org.hibernate.sql.ast.tree.expression.Summarization;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.util.comparator.Comparators;

import lombok.extern.slf4j.Slf4j;
import space.gavinklfong.demo.streamapi.models.Customer;
import space.gavinklfong.demo.streamapi.models.Order;
import space.gavinklfong.demo.streamapi.models.Product;
import space.gavinklfong.demo.streamapi.repos.CustomerRepo;
import space.gavinklfong.demo.streamapi.repos.OrderRepo;
import space.gavinklfong.demo.streamapi.repos.ProductRepo;

@Slf4j
@DataJpaTest
public class MyStreamApiTest {

	@Autowired
	private CustomerRepo customerRepo;

	@Autowired
	private OrderRepo orderRepo;

	@Autowired
	private ProductRepo productRepo;

	@Test
	@DisplayName("Obtain a list of product with category = \"Books\" and price > 100")
	public void exercise1() {
		productRepo.findAll().stream().filter(d -> d.getCategory().equals("Books")).filter(d -> d.getPrice() > 100)
				.collect(Collectors.toList());
	}

	@Test
	@DisplayName("Obtain a list of product with category = \"Books\" and price > 100 (using Predicate chaining for filter)")
	public void exercise1a() {
		Predicate<Product> book = d -> d.getCategory().equals("Books");
		Predicate<Product> price = d -> d.getPrice() > 100;
		productRepo.findAll().stream().filter(book.and(price)).collect(Collectors.toList());
	}

	@Test
	@DisplayName("Obtain a list of product with category = \"Books\" and price > 100 (using BiPredicate for filter)")
	public void exercise1b() {
		BiPredicate<Product, String> book = (p, s) -> p.getCategory().equals(s);
		Predicate<Product> price = d -> d.getPrice() > 100;
		productRepo.findAll().stream().filter(p -> book.test(p, "Book")).filter(price).collect(Collectors.toList());
	}

	@Test
	@DisplayName("Obtain a list of order with product category = \"Baby\"")
	public void exercise2() {
		List<Order> list = productRepo.findAll().stream().filter(t -> t.getCategory().equals("Baby"))
				.flatMap(t -> t.getOrders().stream()).collect(Collectors.toList());
		list.forEach(o -> System.out.println(o.toString()));
	}

	@Test
	@DisplayName("Obtain a list of product with category = “Toys” and then apply 10% discount\"")
	public void exercise3() {

		productRepo.findAll().stream().filter(t -> t.getCategory().equals("Toys"))
				.forEach(t -> t.setPrice(t.getPrice() * 90 / 100));
	}

	@Test
	@DisplayName("Obtain a list of products ordered by customer of tier 2 between 01-Feb-2021 and 01-Apr-2021")
	public void exercise4() {
		productRepo.findAll().stream()
				.filter(t -> t.getOrders().stream()
						.anyMatch(p -> p.getOrderDate().equals(LocalDateTime.now()) && p.getCustomer().getTier() == 2))
				.toList();
	}

	@Test
	@DisplayName("Get the 3 cheapest products of \"Books\" category")
	public void exercise5() {
		List<Product> firstThree = new ArrayList<>();
		List<Product> list = productRepo.findAll().stream().sorted((o1, o2) -> (o1.getPrice().compareTo(o2.getPrice())))
				.peek(p -> firstThree.add(p)).peek(t -> System.out.println(firstThree.size()))
				.takeWhile(t -> firstThree.size() < 3).collect(Collectors.toList());
		firstThree.forEach(o -> System.out.println(o.toString()));

	}

	@Test
	@DisplayName("Get the 3 most recent placed order")
	public void exercise6() {
		List<Order> firstThree = new ArrayList<>();
		List<Order> list = orderRepo.findAll().stream().sorted(Comparator.comparing(Order::getOrderDate).reversed())
				.peek(p -> firstThree.add(p)).peek(t -> System.out.println(firstThree.size()))
				.takeWhile(t -> firstThree.size() < 3).collect(Collectors.toList());
		firstThree.forEach(o -> System.out.println(o.toString()));
	}

	@Test
	@DisplayName("Get a list of products which was ordered on 15-Mar-2021")
	public void exercise7() {
		List<Product> list = orderRepo.findAll().stream()
				.filter(t -> t.getOrderDate().equals(LocalDate.of(2021, 3, 15))).flatMap(o -> o.getProducts().stream())
				.distinct().collect(Collectors.toList());
		list.forEach(o -> System.out.println(o.toString()));

	}

	@Test
	@DisplayName("Calculate the total lump of all orders placed in Feb 2021")
	public void exercise8() {
		Double sum = orderRepo.findAll().stream()
				.filter(t -> t.getOrderDate().getMonth().equals(Month.FEBRUARY) && t.getOrderDate().getYear() == 2021)
				.flatMap(o -> o.getProducts().stream()).mapToDouble(l -> l.getPrice()).sum();
		System.out.println(sum);
	}

	@Test
	@DisplayName("Calculate the total lump of all orders placed in Feb 2021 (using reduce with BiFunction)")
	public void exercise8a() {
		BiFunction<Double, Product, Double> fn = (s, p) -> p.getPrice() + s;
		Double sum = orderRepo.findAll().stream()
				.filter(t -> t.getOrderDate().getMonth().equals(Month.FEBRUARY) && t.getOrderDate().getYear() == 2021)
				.flatMap(p -> p.getProducts().stream()).reduce(0d, fn, Double::sum);
		System.out.println(sum);
	}

	@Test
	@DisplayName("Calculate the average price of all orders placed on 15-Mar-2021")
	public void exercise9() {
		Double sum = orderRepo.findAll().stream().filter(t -> t.getOrderDate().equals(LocalDate.of(2021, 3, 15)))
				.flatMap(o -> o.getProducts().stream()).distinct().mapToDouble(p -> p.getPrice()).average()
				.getAsDouble();

	}

	@Test
	@DisplayName("Obtain statistics summary of all products belong to \"Books\" category")
	public void exercise10() {

		productRepo.findAll().stream().filter(d -> d.getCategory().equals("Books")).mapToDouble(Product::getPrice)
				.summaryStatistics();
	}

	@Test
	@DisplayName("Obtain a mapping of order id and the order's product count")
	public void exercise11() {
		Map<Long, Integer> sum = orderRepo.findAll().stream()
				.collect(Collectors.toMap(Order::getId, order -> order.getProducts().size()));
	}

	@Test
	@DisplayName("Obtain a data map of customer and list of orders")
	public void exercise12() {
		Map<Customer, List<Order>> sum = orderRepo.findAll().stream()
				.collect(Collectors.groupingBy(Order::getCustomer));
	}

	@Test
	@DisplayName("Obtain a data map of customer_id and list of order_id(s)")
	public void exercise12a() {
		Map<Long, List<Long>> sum = orderRepo.findAll().stream().collect(Collectors.groupingBy(
				o -> o.getCustomer().getId(), Collectors.mapping(Order::getId, Collectors.toList())));
	}

	@Test
	@DisplayName("Obtain a data map with order and its total price")
	public void exercise13() {
		Map<Order, Double> sum = orderRepo.findAll().stream()
				.collect(Collectors.groupingBy(Function.identity(), 
						Collectors.summingDouble(k -> k.getProducts().stream().mapToDouble(y -> y.getPrice()).sum())));
	}

	@Test
	@DisplayName("Obtain a data map with order and its total price (using reduce)")
	public void exercise13a() {
		Map<Order, Double> sum = orderRepo.findAll().stream().collect(Collectors.toMap(Function.identity(),
				t -> t.getProducts().stream().reduce(0d, (acc, pro) -> acc + pro.getPrice(), Double::sum)));
	}

	@Test
	@DisplayName("Obtain a data map of product name by category")
	public void exercise14() {
		Map<String, List<String>> map = productRepo.findAll().stream().collect(Collectors.groupingBy(
				p -> p.getCategory(), HashMap::new, Collectors.mapping(pr -> pr.getName(), Collectors.toList())));
	}

	@Test
	@DisplayName("Get the most expensive product per category")
	void exercise15() {
		Map<String, Product> map = productRepo.findAll().stream().collect(Collectors.toMap(Product::getCategory,
				Function.identity(), BinaryOperator.maxBy(Comparator.comparing(Product::getPrice))));
	}

	@Test
	@DisplayName("Get the most expensive product (by name) per category")
	void exercise15a() {
		Map<String, String> map = productRepo.findAll().stream()
				.collect(Collectors.groupingBy(Product::getCategory,
						Collectors.collectingAndThen(Collectors.maxBy(Comparator.comparingDouble(Product::getPrice)),
								p -> p.map(Product::getName).orElse(null))));
	}

}
