package com.torlus.blitzemu;

public class Value {
	public ValueType type = ValueType.UNDEFINED;
	
	public String value = "";
	public int intValue = 0;
	public float floatValue = 0.0f;
	public boolean boolValue = false;
	
	public Value() {
	}
	
	public Value(Token token) {
		switch(token.type) {
		case INTEGER:
			this.type = ValueType.INTEGER;
			this.intValue = token.intValue;
			break;
		case FLOAT:
			this.type = ValueType.FLOAT;
			this.floatValue = token.floatValue;
			break;
		case STRING:
			this.type = ValueType.STRING;
			this.value = token.value;
			break;
		default:
			throw new RuntimeException("Incorrect Token for Value " + token);
		}
	}

	private void error(Token op, Value v) {
		throw new RuntimeException("Unable to apply() for (" + this + ", " + op + ", " + v + ")");
	}

	
	public Value apply(Token op, Value v) {
		switch(type) {
		case INTEGER:
			intApply(op, v);
			break;
		case FLOAT:
			floatApply(op, v);
			break;
		case STRING:
			stringApply(op, v);
			break;
		default:
			error(op, v);
		}
		return this;
	}
	
	
	private void intApply(Token op, Value v) {
		switch(op.type) {
		case PLUS:
			switch(v.type) {
			case INTEGER:
				this.intValue += v.intValue;
				break;
			case FLOAT:
				this.type = ValueType.FLOAT;
				this.floatValue = (float)this.intValue + v.floatValue;
				break;
			case STRING:
				this.type = ValueType.STRING;
				this.value = ("" + this.intValue) + v.value;
				break;
			default:
				error(op, v);
			}
			break;
		case MINUS:
			switch(v.type) {
			case INTEGER:
				this.intValue -= v.intValue;
				break;
			case FLOAT:
				this.type = ValueType.FLOAT;
				this.floatValue = (float)this.intValue - v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case STAR:
			switch(v.type) {
			case INTEGER:
				this.intValue *= v.intValue;
				break;
			case FLOAT:
				this.type = ValueType.FLOAT;
				this.floatValue = (float)this.intValue * v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case SLASH:
			switch(v.type) {
			case INTEGER:
				this.intValue /= v.intValue;
				break;
			case FLOAT:
				this.type = ValueType.FLOAT;
				this.floatValue = (float)this.intValue / v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		default:
			error(op, v);
		}
	}

	private void floatApply(Token op, Value v) {
		switch(op.type) {
		case PLUS:
			switch(v.type) {
			case INTEGER:
				this.floatValue += v.intValue;
				break;
			case FLOAT:
				this.floatValue += v.floatValue;
				break;
			case STRING:
				this.type = ValueType.STRING;
				this.value = ("" + this.floatValue) + v.value;
				break;
			default:
				error(op, v);
			}
			break;
		case MINUS:
			switch(v.type) {
			case INTEGER:
				this.floatValue -= v.intValue;
				break;
			case FLOAT:
				this.floatValue += v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case STAR:
			switch(v.type) {
			case INTEGER:
				this.floatValue *= v.intValue;
				break;
			case FLOAT:
				this.floatValue *= v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case SLASH:
			switch(v.type) {
			case INTEGER:
				this.floatValue /= v.floatValue;
				break;
			case FLOAT:
				this.floatValue /= v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		default:
			error(op, v);
		}
		
	}

	private void stringApply(Token op, Value v) {
		switch(op.type) {
		case PLUS:
			switch(v.type) {
			case INTEGER:
				this.value += "" + v.intValue;
				break;
			case FLOAT:
				this.value += "" + v.floatValue;
				break;
			case STRING:
				this.value += v.value;
				break;
			default:
				error(op, v);
			}
			break;
		default:
			error(op, v);		
		}
	}
	
	public Value negate() {
		switch(type) {
		case INTEGER:
			intValue = -intValue;
			break;
		case FLOAT:
			floatValue = -floatValue;
			break;
		default:
			throw new RuntimeException("Unable to negate " + this);
		}
		return this;		
	}
	
	@Override
	public String toString() {
		switch(type) {
		case BOOLEAN:
			return "[BOOLEAN " + (boolValue ? "True" : "False") + "]";
		case FLOAT:
			return "[FLOAT " + floatValue + "]";
		case INTEGER:
			return "[INTEGER " + intValue + "]";
		case STRING:
			return "[STRING " + value + "]";
		default:
			return "[UNDEFINED]";			
		}
	}

}
