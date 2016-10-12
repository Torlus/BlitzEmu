package com.torlus.blitzemu;

public class Value {
	public ValueType type = ValueType.UNDEFINED;
	
	public String value = "";
	public int intValue = 0;
	public float floatValue = 0.0f;
	
	public Value() {
	}
	
	// Copy Constructor
	public Value(Value other) {
		this.type = other.type;
		this.intValue = other.intValue;
		this.floatValue = other.floatValue;
		this.value = new String(other.value);
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

	public Value(String keyword) {
		this.type = ValueType.STRING;
		this.value = keyword;
	}
	
	private String applyInfo(Token op, Value v) {
		return "(" + this + ", " + op + ", " + v + ")";
	}
	
	private void error(Token op, Value v) {
		throw new RuntimeException("Unable to apply() for " + applyInfo(op, v));
	}

	
	public Value apply(Token op, Value v) {
		// System.out.println(applyInfo(op, v));
		Value res = new Value(this);
		switch(type) {
		case INTEGER:
			res = intApply(op, v);
			break;
		case FLOAT:
			res = floatApply(op, v);
			break;
		case STRING:
			res = stringApply(op, v);
			break;
		default:
			error(op, v);
		}
		// System.out.println("yield " + this);
		return res;
	}
	
	
	private Value intApply(Token op, Value v) {
		Value res = new Value(this);
		switch(op.type) {
		case PLUS:
			switch(v.type) {
			case INTEGER:
				res.intValue += v.intValue;
				break;
			case FLOAT:
				res.type = ValueType.FLOAT;
				res.floatValue = (float)this.intValue + v.floatValue;
				break;
			case STRING:
				res.type = ValueType.STRING;
				res.value = ("" + this.intValue) + v.value;
				break;
			default:
				error(op, v);
			}
			break;
		case MINUS:
			switch(v.type) {
			case INTEGER:
				res.intValue -= v.intValue;
				break;
			case FLOAT:
				res.type = ValueType.FLOAT;
				res.floatValue = (float)this.intValue - v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case STAR:
			switch(v.type) {
			case INTEGER:
				res.intValue *= v.intValue;
				break;
			case FLOAT:
				res.type = ValueType.FLOAT;
				res.floatValue = (float)this.intValue * v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case SLASH:
			switch(v.type) {
			case INTEGER:
				res.intValue /= v.intValue;
				break;
			case FLOAT:
				res.type = ValueType.FLOAT;
				res.floatValue = (float)this.intValue / v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		default:
			error(op, v);
		}
		return res;
	}

	private Value floatApply(Token op, Value v) {
		Value res = new Value(this);
		switch(op.type) {
		case PLUS:
			switch(v.type) {
			case INTEGER:
				res.floatValue += v.intValue;
				break;
			case FLOAT:
				res.floatValue += v.floatValue;
				break;
			case STRING:
				res.type = ValueType.STRING;
				res.value = ("" + this.floatValue) + v.value;
				break;
			default:
				error(op, v);
			}
			break;
		case MINUS:
			switch(v.type) {
			case INTEGER:
				res.floatValue -= v.intValue;
				break;
			case FLOAT:
				res.floatValue -= v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case STAR:
			switch(v.type) {
			case INTEGER:
				res.floatValue *= v.intValue;
				break;
			case FLOAT:
				res.floatValue *= v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		case SLASH:
			switch(v.type) {
			case INTEGER:
				res.floatValue /= v.floatValue;
				break;
			case FLOAT:
				res.floatValue /= v.floatValue;
				break;
			default:
				error(op, v);
			}
			break;
		default:
			error(op, v);
		}
		return res;
	}

	private Value stringApply(Token op, Value v) {
		Value res = new Value(this);
		switch(op.type) {
		case PLUS:
			switch(v.type) {
			case INTEGER:
				res.value += "" + v.intValue;
				break;
			case FLOAT:
				res.value += "" + v.floatValue;
				break;
			case STRING:
				res.value += v.value;
				break;
			default:
				error(op, v);
			}
			break;
		default:
			error(op, v);		
		}
		return res;
	}
	
	public Value negate() {
		Value res = new Value(this);
		switch(type) {
		case INTEGER:
			res.intValue = -intValue;
			break;
		case FLOAT:
			res.floatValue = -floatValue;
			break;
		default:
			throw new RuntimeException("Unable to negate " + this);
		}
		return res;		
	}
	
	@Override
	public String toString() {
		switch(type) {
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

	public void compError(Token op, Value other) {
		throw new RuntimeException("Cannot compare " + this + " " + op + " " + other);
	}
	
	public boolean compare(Token op, Value other) {
		Token minus = new Token(TokenType.MINUS, 0);
		Value diff = apply(minus, other);
		switch(diff.type) {
		case FLOAT:
			switch(op.type) {
			case LT:
				return (diff.floatValue < 0.0f);
			case LE:
				return (diff.floatValue <= 0.0f);
			case NE:
				return (diff.floatValue != 0.0f);
			case EQ:
				return (diff.floatValue == 0.0f);
			case GT:
				return (diff.floatValue > 0.0f);
			case GE:
				return (diff.floatValue >= 0.0f);
			default:
				compError(op, other);
			}
			break;
		case INTEGER:
			switch(op.type) {
			case LT:
			  return (diff.intValue < 0);
			case LE:
			  return (diff.intValue <= 0);
			case NE:
			  return (diff.intValue != 0);
			case EQ:
			  return (diff.intValue == 0);
			case GT:
			  return (diff.intValue > 0);
			case GE:
			  return (diff.intValue >= 0);
			default:
			  compError(op, other);
			}
			break;			
		default:
			compError(op, other);
		}
		return false;
	}
	
}
