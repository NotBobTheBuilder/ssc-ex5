package ex5.utils;

/**
 * An interface for collections to which objects can be pushed.
 *
 * @param <T> the type of the collection
 */
public interface Pushable<T> {

  /**
   * Push a new element onto the object
   *
   * @param _elem the element to push
   */
  public void push(T _elem);

  /**
   * Put an element at a position in the collection
   *
   * @param _elem the element to push
   * @param _pos  the position to put _elem at
   */
  public void put(T _elem, int _pos);

}
